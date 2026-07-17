package com.example.paymenthub.controller;

import com.example.paymenthub.common.base.ApiResponse;
import com.example.paymenthub.common.base.BaseController;
import com.example.paymenthub.dto.request.GroupCategoryDTO;
import com.example.paymenthub.dto.response.GroupCategoryResponseDTO;
import com.example.paymenthub.entity.GroupCategory;
import com.example.paymenthub.service.GroupCategoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/group-category")
public class GroupCategoryController extends BaseController {

    private final GroupCategoryService service;

    public GroupCategoryController(GroupCategoryService service) {
        this.service = service;
    }

    // --- DẠNG 1: JPA & JPA SPECIFICATION ---

    /**
     * Tìm kiếm động phân trang bằng JPA Specification (Dạng 1)
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<GroupCategoryResponseDTO>>> search(
            @RequestParam(required = false) String paramType,
            @RequestParam(required = false) String paramValue,
            @RequestParam(required = false) String paramName,
            @RequestParam(required = false) List<Integer> status,
            @RequestParam(required = false) List<Integer> isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedDate,desc") String sort
    ) {
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction sortDirection = Sort.Direction.DESC;
        if (sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])) {
            sortDirection = Sort.Direction.ASC;
        }

        // Thêm trường sắp xếp phụ theo ID để đảm bảo phân trang ổn định (stable sorting) khi các cột chính trùng giá trị
        Pageable pageable = PageRequest.of(page, size, 
                Sort.by(sortDirection, sortField)
                    .and(Sort.by(Sort.Direction.DESC, "id"))
        );
        Page<GroupCategory> result = service.search(paramType, paramValue, paramName, status, isActive, pageable);
        Page<GroupCategoryResponseDTO> dtoResult = result.map(GroupCategoryResponseDTO::fromEntity);
        return ok(dtoResult, "Lấy danh sách tham số thành công");
    }

    /**
     * Lấy thông tin chi tiết bằng JPA
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GroupCategoryResponseDTO>> getById(@PathVariable Long id) {
        GroupCategory entity = service.getById(id);
        return ok(GroupCategoryResponseDTO.fromEntity(entity), "Lấy chi tiết tham số thành công");
    }

    /**
     * Thêm mới bằng JPA
     */
    @PostMapping
    public ResponseEntity<ApiResponse<GroupCategoryResponseDTO>> create(
            @Valid @RequestBody GroupCategoryDTO dto,
            @RequestHeader(value = "X-Username", defaultValue = "SYSTEM") String username
    ) {
        GroupCategory created = service.create(dto, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(GroupCategoryResponseDTO.fromEntity(created), "Tạo mới tham số thành công"));
    }

    /**
     * Chỉnh sửa bằng JPA (lưu tạm vào NEW_DATA nếu đã duyệt)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GroupCategoryResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody GroupCategoryDTO dto,
            @RequestHeader(value = "X-Username", defaultValue = "SYSTEM") String username
    ) {
        GroupCategory updated = service.update(id, dto, username);
        return ok(GroupCategoryResponseDTO.fromEntity(updated), "Cập nhật tham số thành công");
    }

    /**
     * Xóa bằng JPA
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Username", defaultValue = "SYSTEM") String username
    ) {
        service.delete(id, username);
        return ok(null, "Xóa tham số thành công");
    }

    /**
     * Gửi duyệt bằng JPA
     */
    @PostMapping("/{id}/send-approval")
    public ResponseEntity<ApiResponse<GroupCategoryResponseDTO>> sendApproval(
            @PathVariable Long id,
            @RequestHeader(value = "X-Username", defaultValue = "SYSTEM") String username
    ) {
        GroupCategory updated = service.sendForApproval(id, username);
        return ok(GroupCategoryResponseDTO.fromEntity(updated), "Gửi duyệt tham số thành công");
    }

    // --- DẠNG 2: NATIVE QUERY ---

    /**
     * Truy vấn JOIN nhiều bảng sử dụng Native Query
     */
    @GetMapping("/complex-list")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getComplexList() {
        return ok(service.getJoinedList(), "Lấy danh sách liên kết thành công");
    }

    /**
     * Xuất dữ liệu Excel sử dụng Native Query
     */
    @GetMapping("/export")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> exportData() {
        return ok(service.getRawDataForExport(), "Xuất dữ liệu thành công");
    }

    // --- DẠNG 3: STORED PROCEDURE ---

    /**
     * Duyệt hàng loạt sử dụng Stored Procedure
     */
    @PostMapping("/batch-approve")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> batchApprove(
            @RequestBody List<Long> ids,
            @RequestHeader(value = "X-Username", defaultValue = "APPROVER") String username
    ) {
        List<Map<String, Object>> result = service.batchApprove(ids, username);
        return ok(result, "Phê duyệt hàng loạt thành công");
    }

    /**
     * Từ chối duyệt / Hủy duyệt hàng loạt sử dụng Stored Procedure
     */
    @PostMapping("/batch-reject")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> batchReject(
            @RequestBody List<Long> ids,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "X-Username", defaultValue = "APPROVER") String username
    ) {
        List<Map<String, Object>> result = service.batchReject(ids, reason, username);
        return ok(result, "Từ chối/Hủy duyệt hàng loạt thành công");
    }
}
