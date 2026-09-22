package com.example.paymenthub.service.export;

import com.example.paymenthub.dto.request.CreateExportCategoryJobRequestDTO;
import com.example.paymenthub.dto.request.CreateExportComponentJobRequestDTO;
import com.example.paymenthub.dto.request.CreateExportJobRequestDTO;
import com.example.paymenthub.dto.response.ExportJobResponseDTO;

import java.util.List;

/**
 * Interface quản lý vòng đời Export Job.
 * userId dùng kiểu Long (= PMH_APP_USERS.ID) để khớp với cột USER_ID (NUMBER) trên Oracle DB.
 *
 * Hỗ trợ 3 module: TRANSACTION, CATEGORY, COMPONENT.
 * Tất cả đều dùng cùng bảng EXPORT_JOB, phân biệt qua filterPayload.
 */
public interface ExportJobService {

    /**
     * Tạo job export Transaction. Trả về HTTP 202 Accepted trong < 100ms.
     * Thứ tự kiểm tra:
     *   1. Lookup User.id từ username → userId (Long)
     *   2. Per-User Lock (Redis SET NX) → HTTP 409 nếu đã có job đang chạy
     *   3. ThreadPool capacity → HTTP 503 nếu queue đầy
     *   4. MAX(ID) snapshot → INSERT PENDING → submit @Async → return 202
     *
     * @param username lấy từ SecurityContext (SecurityUtils.getCurrentUsername())
     * @param request  bộ lọc từ Frontend
     */
    ExportJobResponseDTO createJob(String username, CreateExportJobRequestDTO request);

    /**
     * Tạo job export Category. Trả về HTTP 202 Accepted trong < 100ms.
     * Cơ chế giống Transaction, không cần MAX_EXPORT_ID (bảng nhỏ, không snapshot).
     *
     * @param username lấy từ SecurityContext
     * @param request  bộ lọc Category từ Frontend
     */
    ExportJobResponseDTO createCategoryJob(String username, CreateExportCategoryJobRequestDTO request);

    /**
     * Tạo job export Component. Trả về HTTP 202 Accepted trong < 100ms.
     *
     * @param username lấy từ SecurityContext
     * @param request  bộ lọc Component từ Frontend
     */
    ExportJobResponseDTO createComponentJob(String username, CreateExportComponentJobRequestDTO request);

    /**
     * Lấy job đang active (PENDING/PROCESSING) của user.
     * Tiến độ đọc từ Redis Cache — không query Oracle DB.
     * Dùng cho Frontend polling mỗi 3 giây.
     *
     * @param username lấy từ SecurityContext
     * @return null nếu không có job nào đang chạy
     */
    ExportJobResponseDTO getActiveJob(String username);

    /**
     * Lấy danh sách job trong 12 tiếng gần nhất của user.
     * Dùng cho Header Notification Bell.
     *
     * @param username lấy từ SecurityContext
     */
    List<ExportJobResponseDTO> getMyJobs(String username);

    /**
     * Sinh URL tải file cho job đã COMPLETED.
     * Bắt buộc kiểm tra Ownership (Anti-IDOR): job.userId == currentUser.id.
     *
     * @param jobId    ID (Long) của job
     * @param username lấy từ SecurityContext (để lookup userId kiểm tra ownership)
     */
    String getDownloadUrl(Long jobId, String username);
}
