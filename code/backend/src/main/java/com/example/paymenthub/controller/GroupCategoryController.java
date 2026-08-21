package com.example.paymenthub.controller;

import com.example.paymenthub.common.base.ApiResponse;
import com.example.paymenthub.common.base.BaseController;
import com.example.paymenthub.dto.request.GroupCategoryDTO;
import com.example.paymenthub.dto.request.GroupCategorySearchCriteria;
import com.example.paymenthub.dto.response.BatchItemResultDTO;
import com.example.paymenthub.dto.response.GroupCategoryResponseDTO;
import com.example.paymenthub.entity.GroupCategory;
import com.example.paymenthub.security.SecurityUtils;
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

    /**
     * Tìm kiếm động phân trang bằng JPA Specification
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('MAKER', 'CHECKER')")
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
    @PreAuthorize("hasAnyRole('MAKER', 'CHECKER')")
    public ResponseEntity<ApiResponse<GroupCategoryResponseDTO>> getById(@PathVariable Long id) {
        GroupCategory entity = service.getById(id);
        return ok(GroupCategoryResponseDTO.fromEntity(entity), "Lấy chi tiết tham số thành công");
    }

    /**
     * Thêm mới (Quyền MAKER)
     */
    @PostMapping
    @PreAuthorize("hasRole('MAKER')")
    public ResponseEntity<ApiResponse<GroupCategoryResponseDTO>> create(@Valid @RequestBody GroupCategoryDTO dto) {
        String username = SecurityUtils.getCurrentUsername();
        GroupCategory created = service.create(dto, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(GroupCategoryResponseDTO.fromEntity(created), "Tạo mới tham số thành công"));
    }

    /**
     * Chỉnh sửa (Quyền MAKER)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MAKER')")
    public ResponseEntity<ApiResponse<GroupCategoryResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody GroupCategoryDTO dto
    ) {
        String username = SecurityUtils.getCurrentUsername();
        GroupCategory updated = service.update(id, dto, username);
        return ok(GroupCategoryResponseDTO.fromEntity(updated), "Cập nhật tham số thành công");
    }

    /**
     * Xóa (Quyền MAKER) - Trả về ApiResponse đồng nhất với Frontend Angular
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MAKER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        String username = SecurityUtils.getCurrentUsername();
        service.delete(id, username);
        return ok(null, "Xóa tham số thành công");
    }

    /**
     * Gửi duyệt (Quyền MAKER)
     */
    @PostMapping("/{id}/send-approval")
    @PreAuthorize("hasRole('MAKER')")
    public ResponseEntity<ApiResponse<GroupCategoryResponseDTO>> sendApproval(@PathVariable Long id) {
        String username = SecurityUtils.getCurrentUsername();
        GroupCategory updated = service.sendForApproval(id, username);
        return ok(GroupCategoryResponseDTO.fromEntity(updated), "Gửi duyệt tham số thành công");
    }

    /**
     * Hủy duyệt (Quyền MAKER)
     */
    @PostMapping("/{id}/cancel-approval")
    @PreAuthorize("hasRole('MAKER')")
    public ResponseEntity<ApiResponse<GroupCategoryResponseDTO>> cancelApproval(@PathVariable Long id) {
        String username = SecurityUtils.getCurrentUsername();
        GroupCategory updated = service.cancelApproval(id, username);
        return ok(GroupCategoryResponseDTO.fromEntity(updated), "Hủy duyệt tham số thành công");
    }

    /**
     * Truy vấn JOIN nhiều bảng sử dụng Native Query
     */
    @GetMapping("/complex-list")
    @PreAuthorize("hasAnyRole('MAKER', 'CHECKER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getComplexList() {
        return ok(service.getJoinedList(), "Lấy danh sách liên kết thành công");
    }

    /**
     * Xuất dữ liệu Excel
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('MAKER', 'CHECKER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> exportData() {
        return ok(service.getRawDataForExport(), "Xuất dữ liệu thành công");
    }

    /**
     * Duyệt hàng loạt (Quyền CHECKER)
     */
    @PostMapping("/batch-approve")
    @PreAuthorize("hasRole('CHECKER')")
    public ResponseEntity<ApiResponse<List<BatchItemResultDTO>>> batchApprove(@RequestBody List<Long> ids) {
        String username = SecurityUtils.getCurrentUsername();
        List<BatchItemResultDTO> result = service.batchApprove(ids, username);
        return ok(result, "Phê duyệt hàng loạt thành công");
    }

    /**
     * Từ chối duyệt hàng loạt (Quyền CHECKER)
     */
    @PostMapping("/batch-reject")
    @PreAuthorize("hasRole('CHECKER')")
    public ResponseEntity<ApiResponse<List<BatchItemResultDTO>>> batchReject(
            @RequestBody List<Long> ids,
            @RequestParam(required = false) String reason
    ) {
        String username = SecurityUtils.getCurrentUsername();
        List<BatchItemResultDTO> result = service.batchReject(ids, reason, username);
        return ok(result, "Từ chối/Hủy duyệt hàng loạt thành công");
    }
}
