package com.example.paymenthub.controller;

import com.example.paymenthub.common.base.ApiResponse;
import com.example.paymenthub.common.base.BaseController;
import com.example.paymenthub.dto.request.CreateExportJobRequestDTO;
import com.example.paymenthub.dto.request.TransactionLogSearchCriteria;
import com.example.paymenthub.dto.response.ExportJobResponseDTO;
import com.example.paymenthub.dto.response.TransactionLogResponseDTO;
import com.example.paymenthub.entity.TransactionLog;
import com.example.paymenthub.service.export.ExportJobService;
import com.example.paymenthub.service.TransactionLogService;
import com.example.paymenthub.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transaction-log")
@RequiredArgsConstructor
@Slf4j
public class TransactionLogController extends BaseController {

    private final TransactionLogService transactionLogService;
    private final ExportJobService exportJobService;

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('TRANSACTION_VIEW', 'ROLE_MAKER', 'ROLE_CHECKER', 'ROLE_ADMIN', 'AUDIT_VIEW')")
    public ResponseEntity<ApiResponse<Page<TransactionLogResponseDTO>>> search(
            @ModelAttribute @Valid TransactionLogSearchCriteria criteria,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        boolean hasIdSort = pageable.getSort().stream()
                .anyMatch(order -> order.getProperty().equalsIgnoreCase("id"));

        Pageable pageableWithFallback = hasIdSort ? pageable
                : PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSort().and(Sort.by(Sort.Direction.DESC, "id")));

        Page<TransactionLog> result = transactionLogService.search(criteria, pageableWithFallback);
        Page<TransactionLogResponseDTO> dtoResult = result.map(TransactionLogResponseDTO::fromEntity);
        return ok(dtoResult, "Lấy danh sách nhật ký giao dịch thành công");
    }

    @PostMapping("/export-jobs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyAuthority('TRANSACTION_EXPORT', 'ROLE_MAKER', 'ROLE_CHECKER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<ExportJobResponseDTO>> createExportJob(
            @RequestBody CreateExportJobRequestDTO request) {

        String userId = SecurityUtils.getCurrentUsername();
        ExportJobResponseDTO job = exportJobService.createJob(userId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(job, "Yêu cầu xuất file đã được tiếp nhận. Hệ thống đang xử lý..."));
    }

    @GetMapping("/export-jobs/active")
    @PreAuthorize("hasAnyAuthority('TRANSACTION_VIEW', 'ROLE_MAKER', 'ROLE_CHECKER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<ExportJobResponseDTO>> getActiveJob() {
        String userId = SecurityUtils.getCurrentUsername();
        ExportJobResponseDTO job = exportJobService.getActiveJob(userId);
        return ok(job, job != null ? "Đang xử lý yêu cầu xuất file" : "Không có yêu cầu xuất file đang xử lý");
    }

    @GetMapping("/export-jobs/my-jobs")
    @PreAuthorize("hasAnyAuthority('TRANSACTION_VIEW', 'ROLE_MAKER', 'ROLE_CHECKER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<ExportJobResponseDTO>>> getMyJobs() {
        String userId = SecurityUtils.getCurrentUsername();
        List<ExportJobResponseDTO> jobs = exportJobService.getMyJobs(userId);
        return ok(jobs, "Danh sách yêu cầu xuất file trong 12 giờ gần nhất");
    }

    @GetMapping("/export-jobs/{jobId}/download")
    @PreAuthorize("hasAnyAuthority('TRANSACTION_VIEW', 'ROLE_MAKER', 'ROLE_CHECKER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<String>> getDownloadUrl(@PathVariable Long jobId) {
        String userId = SecurityUtils.getCurrentUsername();
        String downloadUrl = exportJobService.getDownloadUrl(jobId, userId);
        return ok(downloadUrl, "URL tải file đã sẵn sàng");
    }

}
