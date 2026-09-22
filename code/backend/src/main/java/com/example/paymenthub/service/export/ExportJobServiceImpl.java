package com.example.paymenthub.service.export;

import com.example.paymenthub.common.enums.ExportJobStatus;
import com.example.paymenthub.common.exception.CustomBusinessException;
import com.example.paymenthub.common.exception.ForbiddenAccessException;
import com.example.paymenthub.common.exception.ResourceNotFoundException;
import com.example.paymenthub.common.util.JsonUtils;
import com.example.paymenthub.dto.request.CreateExportCategoryJobRequestDTO;
import com.example.paymenthub.dto.request.CreateExportComponentJobRequestDTO;
import com.example.paymenthub.dto.request.CreateExportJobRequestDTO;
import com.example.paymenthub.dto.response.ExportJobResponseDTO;
import com.example.paymenthub.entity.ExportJob;
import com.example.paymenthub.entity.User;
import com.example.paymenthub.repository.ExportJobRepository;
import com.example.paymenthub.repository.UserRepository;
import com.example.paymenthub.service.FileStorageService;
import com.example.paymenthub.service.export.worker.CategoryExportWorker;
import com.example.paymenthub.service.export.worker.ComponentExportWorker;
import com.example.paymenthub.service.export.worker.TransactionExportWorker;
import com.example.paymenthub.repository.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportJobServiceImpl implements ExportJobService {

    private final ExportJobRepository exportJobRepository;
    private final UserRepository userRepository;
    private final ExportLockService exportLockService;
    private final ExportProgressService exportProgressService;
    private final TransactionExportWorker transactionExportWorker;
    private final CategoryExportWorker categoryExportWorker;
    private final ComponentExportWorker componentExportWorker;
    private final FileStorageService fileStorageService;
    private final TransactionLogRepository transactionLogRepository;

    /**
     * NHIỆM VỤ 1: Tiếp nhận yêu cầu xuất dữ liệu giao dịch (Transaction Logs).
     * Chốt MAX(ID) làm ranh giới snapshot và chuyển cho hàm khởi tạo chung
     * createExportJobInternal.
     */
    @Override
    @Transactional
    public ExportJobResponseDTO createJob(String username, CreateExportJobRequestDTO request) {
        Long maxExportId = transactionLogRepository.findMaxId().orElse(null);
        if (maxExportId == null) {
            throw new CustomBusinessException("EXPORT_EMPTY_TABLE",
                    "Bảng dữ liệu giao dịch chưa có bản ghi nào để xuất.");
        }
        return createExportJobInternal(username, request, maxExportId, transactionExportWorker::runExport,
                "Transaction");
    }

    /**
     * NHIỆM VỤ 2: Phục vụ Polling tiến độ real-time cho Frontend (mỗi 3 giây).
     * Đọc % tiến độ trực tiếp từ Redis Cache qua exportProgressService để không
     * query Oracle DB.
     */
    @Override
    @Transactional(readOnly = true)
    public ExportJobResponseDTO getActiveJob(String username) {
        Long userId = resolveUserId(username);
        if (userId == null)
            return null;

        return exportJobRepository.findActiveJobByUserId(userId)
                .map(job -> {
                    // tiến độ từ Redis
                    long[] progress = exportProgressService.getProgress(String.valueOf(job.getId()));
                    if (progress[1] > 0) {
                        job.setProcessedRows(progress[0]);
                        job.setTotalRows(progress[1]);
                    }
                    return ExportJobResponseDTO.fromEntity(job);
                })
                .orElse(null);
    }

    /**
     * NHIỆM VỤ 3: Lấy danh sách các file xuất trong 12 tiếng gần nhất của User.
     * Dùng hiển thị cho Icon Quả chuông (Notification Bell) ở Header giao diện.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ExportJobResponseDTO> getMyJobs(String username) {
        Long userId = resolveUserId(username);
        if (userId == null)
            return List.of();

        LocalDateTime since = LocalDateTime.now().minusHours(12);
        return exportJobRepository.findRecentJobsByUserId(userId, since)
                .stream()
                .map(ExportJobResponseDTO::fromEntity)
                .toList();
    }

    /**
     * NHIỆM VỤ 4: Phân quyền bảo mật & Sinh URL tải file an toàn.
     * Kiểm tra Anti-IDOR (chỉ được tải file của chính mình), kiểm tra file còn hạn
     * 12h,
     * sau đó tạo Presigned URL để trình duyệt tải trực tiếp từ MinIO/Storage.
     */
    @Override
    @Transactional
    public String getDownloadUrl(Long jobId, String username) {
        ExportJob job = exportJobRepository.findById(jobId)
                .orElseThrow(() -> new CustomBusinessException("JOB_NOT_FOUND",
                        "Không tìm thấy yêu cầu xuất file."));

        // Anti-IDOR: Kiểm tra Ownership — so sánh job.userId (Long) với User.id (Long)
        Long userId = resolveUserId(username);
        if (userId == null || !job.getUserId().equals(userId)) {
            throw new ForbiddenAccessException("Bạn không có quyền tải file này.");
        }

        // Kiểm tra job đã hoàn thành thành công chưa — dùng isSuccess() để tương thích
        // với DONE/SUCCESS/COMPLETED
        ExportJobStatus jobStatus;
        try {
            jobStatus = ExportJobStatus.valueOf(job.getStatus());
        } catch (IllegalArgumentException e) {
            throw new CustomBusinessException("JOB_NOT_COMPLETED", "File xuất chưa sẵn sàng để tải về.");
        }
        if (!jobStatus.isSuccess()) {
            throw new CustomBusinessException("JOB_NOT_COMPLETED",
                    "File xuất chưa sẵn sàng để tải về.");
        }

        if (job.getExportUrl() == null || job.getExportUrl().isBlank()) {
            throw new CustomBusinessException("JOB_EXPIRED",
                    "File xuất đã được tự động xóa khỏi máy chủ. Vui lòng tạo yêu cầu xuất mới.");
        }

        if (job.getExpiresAt() != null && job.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new CustomBusinessException("JOB_EXPIRED",
                    "File xuất đã hết hạn. Vui lòng tạo yêu cầu xuất mới.");
        }

        // Lần tải đầu: Tăng downloadCount = 1 và rút ngắn expiresAt thành 30 phút tính từ bây giờ
        if (job.getDownloadCount() == 0) {
            job.setDownloadCount(1);
            job.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        } else {
            job.setDownloadCount(job.getDownloadCount() + 1);
        }
        job.setUpdatedAt(LocalDateTime.now());
        exportJobRepository.save(job);

        try {
            return fileStorageService.generateDownloadUrl(job.getExportUrl(), job.getExpiresAt());
        } catch (Exception e) {
            log.error("[ExportJobService] Failed to generate download URL for jobId={}: {}", jobId, e.getMessage());
            throw new CustomBusinessException("DOWNLOAD_URL_ERROR",
                    "Không thể tạo đường dẫn tải file. Vui lòng thử lại.");
        }
    }

    @Override
    @Transactional
    public ExportJobResponseDTO createCategoryJob(String username, CreateExportCategoryJobRequestDTO request) {
        return createExportJobInternal(username, request, 0L, categoryExportWorker::runExport, "Category");
    }

    @Override
    @Transactional
    public ExportJobResponseDTO createComponentJob(String username, CreateExportComponentJobRequestDTO request) {
        return createExportJobInternal(username, request, 0L, componentExportWorker::runExport, "Component");
    }

    /**
     * HÀM TRUNG TÂM XỬ LÝ KHỞI TẠO JOB:
     * 1. Kiểm tra khóa Redis chống spam theo User (Per-user lock).
     * 2. Lưu bản ghi ExportJob mới với trạng thái PENDING vào DB.
     * 3. Submit tác vụ chạy ngầm vào ThreadPool 3 luồng cho Worker xử lý.
     * 4. Bắt lỗi quá tải ThreadPool (trả 503) và tự giải phóng khóa nếu có lỗi.
     */
    private ExportJobResponseDTO createExportJobInternal(
            String username,
            Object filterRequest,
            Long maxExportId,
            java.util.function.Consumer<Long> workerTask,
            String jobTypeName) {

        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản người dùng: " + username));
        Long userId = user.getId();

        if (!exportLockService.tryLock(String.valueOf(userId))) {
            throw new CustomBusinessException("EXPORT_JOB_ALREADY_RUNNING",
                    "Đang có yêu cầu xuất file đang xử lý. Vui lòng chờ hoàn thành trước khi tạo yêu cầu mới.");
        }

        try {
            ExportJob job = ExportJob.builder()
                    .userId(userId)
                    .filterPayload(JsonUtils.toJson(filterRequest))
                    .status(ExportJobStatus.PENDING.name())
                    .processedRows(0L)
                    .maxExportId(maxExportId != null ? maxExportId : 0L)
                    .lastHeartbeatAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            exportJobRepository.save(job);
            log.info("[ExportJobService] {} job created: id={}, userId={}, maxExportId={}", jobTypeName, job.getId(),
                    userId, maxExportId);

            try {
                workerTask.accept(job.getId());
            } catch (RejectedExecutionException e) {
                job.setStatus(ExportJobStatus.FAILED.name());
                job.setErrorMessage("Hệ thống đang bận. Vui lòng thử lại sau vài phút.");
                job.setUpdatedAt(LocalDateTime.now());
                exportJobRepository.save(job);
                exportLockService.unlock(String.valueOf(userId));
                throw new CustomBusinessException("EXPORT_SYSTEM_BUSY",
                        "Hệ thống đang xử lý quá nhiều yêu cầu xuất file. Vui lòng thử lại sau vài phút.");
            }

            return ExportJobResponseDTO.fromEntity(job);

        } catch (CustomBusinessException e) {
            throw e;
        } catch (Exception e) {
            exportLockService.unlock(String.valueOf(userId));
            log.error("[ExportJobService] Unexpected error creating {} job for user {}: {}", jobTypeName, username,
                    e.getMessage(), e);
            throw new CustomBusinessException("EXPORT_INIT_ERROR",
                    "Không thể khởi tạo yêu cầu xuất file. Vui lòng thử lại.");
        }
    }

    /**
     * Resolve username → User.id (Long). Trả null nếu không tìm thấy (tránh ném
     * exception ở getActiveJob).
     */
    private Long resolveUserId(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .map(User::getId)
                .orElse(null);
    }
}
