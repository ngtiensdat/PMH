package com.example.paymenthub.controller;

import com.example.paymenthub.common.base.ApiResponse;
import com.example.paymenthub.common.base.BaseController;
import com.example.paymenthub.dto.request.GroupCategoryDTO;
import com.example.paymenthub.dto.request.GroupCategorySearchCriteria;
import com.example.paymenthub.dto.request.CreateExportCategoryJobRequestDTO;
import com.example.paymenthub.dto.response.BatchItemResultDTO;
import com.example.paymenthub.dto.response.ExportJobResponseDTO;
import com.example.paymenthub.dto.response.GroupCategoryResponseDTO;
import com.example.paymenthub.entity.GroupCategory;
import com.example.paymenthub.security.SecurityUtils;
import com.example.paymenthub.service.export.ExportJobService;
import com.example.paymenthub.service.GroupCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import java.util.Map;

@RestController
@RequestMapping("/api/group-category")
@RequiredArgsConstructor
public class GroupCategoryController extends BaseController {

    private final GroupCategoryService service;
    private final ExportJobService exportJobService;

    /**
     * Tìm kiếm động phân trang bằng JPA Specification
     */
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('CATEGORY_VIEW')")
    public ResponseEntity<ApiResponse<Page<GroupCategoryResponseDTO>>> search(
            @Valid @ModelAttribute GroupCategorySearchCriteria criteria,
            @PageableDefault(page = 0, size = 10, sort = "updatedDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Pageable pageableWithFallback = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().and(Sort.by(Sort.Direction.DESC, "id"))
        );
        Page<GroupCategory> result = service.search(criteria, pageableWithFallback);
        Page<GroupCategoryResponseDTO> dtoResult = result.map(GroupCategoryResponseDTO::fromEntity);
        return ok(dtoResult, "Lấy danh sách tham số thành công");
    }

    /**
     * Lấy thông tin chi tiết
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CATEGORY_VIEW')")
    public ResponseEntity<ApiResponse<GroupCategoryResponseDTO>> getById(@PathVariable Long id) {
        GroupCategory entity = service.getById(id);
        return ok(GroupCategoryResponseDTO.fromEntity(entity), "Lấy chi tiết tham số thành công");
    }

    /**
     * Thêm mới
     */
    @PostMapping
    @PreAuthorize("hasAuthority('CATEGORY_CREATE')")
    public ResponseEntity<ApiResponse<GroupCategoryResponseDTO>> create(@Valid @RequestBody GroupCategoryDTO dto) {
        String username = SecurityUtils.getCurrentUsername();
        GroupCategory created = service.create(dto, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(GroupCategoryResponseDTO.fromEntity(created), "Tạo mới tham số thành công"));
    }

    /**
     * Chỉnh sửa
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CATEGORY_UPDATE')")
    public ResponseEntity<ApiResponse<GroupCategoryResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody GroupCategoryDTO dto
    ) {
        String username = SecurityUtils.getCurrentUsername();
        GroupCategory updated = service.update(id, dto, username);
        return ok(GroupCategoryResponseDTO.fromEntity(updated), "Cập nhật tham số thành công");
    }

    /**
     * Xóa
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CATEGORY_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        String username = SecurityUtils.getCurrentUsername();
        service.delete(id, username);
        return ok(null, "Xóa tham số thành công");
    }

    /**
     * Gửi duyệt
     */
    @PostMapping("/{id}/send-approval")
    @PreAuthorize("hasAuthority('CATEGORY_SEND')")
    public ResponseEntity<ApiResponse<GroupCategoryResponseDTO>> sendApproval(@PathVariable Long id) {
        String username = SecurityUtils.getCurrentUsername();
        GroupCategory updated = service.sendForApproval(id, username);
        return ok(GroupCategoryResponseDTO.fromEntity(updated), "Gửi duyệt tham số thành công");
    }

    /**
     * Hủy duyệt
     */
    @PostMapping("/{id}/cancel-approval")
    @PreAuthorize("hasAuthority('CATEGORY_CANCEL')")
    public ResponseEntity<ApiResponse<GroupCategoryResponseDTO>> cancelApproval(@PathVariable Long id) {
        String username = SecurityUtils.getCurrentUsername();
        GroupCategory updated = service.cancelApproval(id, username);
        return ok(GroupCategoryResponseDTO.fromEntity(updated), "Hủy duyệt tham số thành công");
    }

    /**
     * Truy vấn JOIN nhiều bảng sử dụng Native Query
     */
    @GetMapping("/complex-list")
    @PreAuthorize("hasAuthority('CATEGORY_VIEW')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getComplexList() {
        return ok(service.getJoinedList(), "Lấy danh sách liên kết thành công");
    }

    /**
     * Đẳy dữ liệu sang file Excel (legacy sync — giữ lại để tương thích ngược)
     */
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('CATEGORY_VIEW')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> exportData() {
        return ok(service.getRawDataForExport(), "Xuất dữ liệu thành công");
    }

    // ── Async Export Job Endpoints (cơ chế giống Transaction) ──────────────────

    /**
     * Tạo Async Export Job cho Category.
     * Trả về HTTP 202 Accepted trong < 100ms.
     */
    @PostMapping("/export-jobs")
    @PreAuthorize("hasAuthority('CATEGORY_VIEW')")
    public ResponseEntity<ApiResponse<ExportJobResponseDTO>> createExportJob(
            @RequestBody(required = false) CreateExportCategoryJobRequestDTO request) {
        String username = SecurityUtils.getCurrentUsername();
        ExportJobResponseDTO dto = exportJobService.createCategoryJob(
                username,
                request != null ? request : new CreateExportCategoryJobRequestDTO());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(dto, "Đã nhận yêu cầu xuất file. Hệ thống đang xử lý..."));
    }

    /**
     * Lấy tiến độ job đang chạy của user (PENDING/PROCESSING).
     * Frontend polling mỗi 3 giây.
     */
    @GetMapping("/export-jobs/active")
    @PreAuthorize("hasAuthority('CATEGORY_VIEW')")
    public ResponseEntity<ApiResponse<ExportJobResponseDTO>> getActiveExportJob() {
        String username = SecurityUtils.getCurrentUsername();
        ExportJobResponseDTO dto = exportJobService.getActiveJob(username);
        return ok(dto, dto != null ? "Đang xử lý" : "Không có job đang chạy");
    }

    /**
     * Lấy danh sách job trong 12 tiếng gần nhất của user.
     */
    @GetMapping("/export-jobs/my-jobs")
    @PreAuthorize("hasAuthority('CATEGORY_VIEW')")
    public ResponseEntity<ApiResponse<List<ExportJobResponseDTO>>> getMyExportJobs() {
        String username = SecurityUtils.getCurrentUsername();
        return ok(exportJobService.getMyJobs(username), "Lấy danh sách xuất file thành công");
    }

    /**
     * Sinh URL tải file (MinIO Presigned URL hoặc Local URL).
     * Kiểm tra Ownership — chỉ chủ nhân job mới được tải.
     */
    @GetMapping("/export-jobs/{jobId}/download")
    @PreAuthorize("hasAuthority('CATEGORY_VIEW')")
    public ResponseEntity<ApiResponse<String>> downloadExportFile(@PathVariable Long jobId) {
        String username = SecurityUtils.getCurrentUsername();
        String url = exportJobService.getDownloadUrl(jobId, username);
        return ok(url, "Lấy đường dẫn tải file thành công");
    }

    /**
     * Duyệt hàng loạt
     */
    @PostMapping("/batch-approve")
    @PreAuthorize("hasAuthority('CATEGORY_APPROVE')")
    public ResponseEntity<ApiResponse<List<BatchItemResultDTO>>> batchApprove(@RequestBody List<Long> ids) {
        return handleBatchResult(service.batchApprove(ids, SecurityUtils.getCurrentUsername()),
                "Phê duyệt hàng loạt hoàn tất", "Phê duyệt thất bại");
    }

    /**
     * Từ chối duyệt hàng loạt
     */
    @PostMapping("/batch-reject")
    @PreAuthorize("hasAuthority('CATEGORY_REJECT')")
    public ResponseEntity<ApiResponse<List<BatchItemResultDTO>>> batchReject(
            @RequestBody List<Long> ids,
            @RequestParam(required = false) String reason
    ) {
        return handleBatchResult(service.batchReject(ids, reason, SecurityUtils.getCurrentUsername()),
                "Từ chối/Hủy duyệt hàng loạt hoàn tất", "Từ chối thất bại");
    }
}
