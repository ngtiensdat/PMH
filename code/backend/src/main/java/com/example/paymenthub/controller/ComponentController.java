package com.example.paymenthub.controller;

import com.example.paymenthub.common.base.ApiResponse;
import com.example.paymenthub.common.base.BaseController;
import com.example.paymenthub.dto.request.ComponentDTO;
import com.example.paymenthub.dto.response.ComponentResponseDTO;
import com.example.paymenthub.entity.ProcessingComponent;
import com.example.paymenthub.service.ComponentService;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/components")
public class ComponentController extends BaseController {

    private final ComponentService service;

    public ComponentController(ComponentService service) {
        this.service = service;
    }

    // ─── DẠNG 1: JPA & JPA SPECIFICATION ──────────────────────────────────────

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ComponentResponseDTO>>> search(
            @RequestParam(required = false) String componentCode,
            @RequestParam(required = false) String componentName,
            @RequestParam(required = false) String messageType,
            @RequestParam(required = false) String connectionMethod,
            @RequestParam(required = false) List<Integer> status,
            @RequestParam(required = false) List<Integer> isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedDate,desc") String sort
    ) {
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = (sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1]))
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        // Thêm trường sắp xếp phụ theo COMPONENT_CODE để đảm bảo phân trang ổn định (stable sorting) khi các cột chính trùng giá trị
        Pageable pageable = PageRequest.of(page, size, 
                Sort.by(direction, sortField)
                    .and(Sort.by(Sort.Direction.DESC, "componentCode"))
        );
        Page<ProcessingComponent> result = service.search(
                componentCode, componentName, messageType, connectionMethod, status, isActive, pageable);
        Page<ComponentResponseDTO> dtoResult = result.map(ComponentResponseDTO::fromEntity);
        return ok(dtoResult, "Lấy danh sách cấu phần thành công");
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<ComponentResponseDTO>> getByCode(@PathVariable String code) {
        ProcessingComponent entity = service.getByCode(code);
        return ok(ComponentResponseDTO.fromEntity(entity), "Lấy chi tiết cấu phần thành công");
    }

    /**
     * Lấy danh sách cấu phần đang hoạt động cho dropdown tại màn Danh mục theo nhóm
     */
    @GetMapping("/active-list")
    public ResponseEntity<ApiResponse<List<ComponentResponseDTO>>> getActiveList(
            @RequestParam(value = "status", required = false) Integer status
    ) {
        List<ProcessingComponent> list = service.getActiveList(status);
        List<ComponentResponseDTO> dtoList = list.stream()
                .map(ComponentResponseDTO::fromEntity)
                .collect(Collectors.toList());
        return ok(dtoList, "Lấy danh sách cấu phần đang hoạt động thành công");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ComponentResponseDTO>> create(
            @Valid @RequestBody ComponentDTO dto,
            @RequestHeader(value = "X-Username", required = false) String username
    ) {
        validateUsername(username);
        ProcessingComponent created = service.create(dto, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ComponentResponseDTO.fromEntity(created), "Tạo mới cấu phần thành công"));
    }

    @PutMapping("/{code}")
    public ResponseEntity<ApiResponse<ComponentResponseDTO>> update(
            @PathVariable String code,
            @Valid @RequestBody ComponentDTO dto,
            @RequestHeader(value = "X-Username", required = false) String username
    ) {
        validateUsername(username);
        ProcessingComponent updated = service.update(code, dto, username);
        return ok(ComponentResponseDTO.fromEntity(updated), "Cập nhật cấu phần thành công");
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(
            @PathVariable String code,
            @RequestHeader(value = "X-Username", required = false) String username
    ) {
        validateUsername(username);
        service.delete(code, username);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{code}/send-approval")
    public ResponseEntity<ApiResponse<ComponentResponseDTO>> sendApproval(
            @PathVariable String code,
            @RequestHeader(value = "X-Username", required = false) String username
    ) {
        validateUsername(username);
        ProcessingComponent updated = service.sendForApproval(code, username);
        return ok(ComponentResponseDTO.fromEntity(updated), "Gửi duyệt cấu phần thành công");
    }

    /**
     * Hủy duyệt bằng JPA
     */
    @PostMapping("/{code}/cancel-approval")
    public ResponseEntity<ApiResponse<ComponentResponseDTO>> cancelApproval(
            @PathVariable String code,
            @RequestHeader(value = "X-Username", required = false) String username
    ) {
        validateUsername(username);
        ProcessingComponent updated = service.cancelApproval(code, username);
        return ok(ComponentResponseDTO.fromEntity(updated), "Hủy duyệt cấu phần thành công");
    }

    // ─── DẠNG 2: NATIVE QUERY ──────────────────────────────────────────────────

    @GetMapping("/export")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> exportData() {
        return ok(service.getRawDataForExport(), "Xuất dữ liệu thành công");
    }

    // ─── DẠNG 3: STORED PROCEDURE ──────────────────────────────────────────────

    @PostMapping("/batch-approve")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> batchApprove(
            @RequestBody List<String> codes,
            @RequestHeader(value = "X-Username", required = false) String username
    ) {
        validateUsername(username);
        return ok(service.batchApprove(codes, username), "Phê duyệt hàng loạt thành công");
    }

    @PostMapping("/batch-reject")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> batchReject(
            @RequestBody List<String> codes,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "X-Username", required = false) String username
    ) {
        validateUsername(username);
        return ok(service.batchReject(codes, reason, username), "Từ chối/Hủy duyệt hàng loạt thành công");
    }

    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new com.example.paymenthub.common.exception.UnauthorizedAccessException("Yêu cầu cần có Header X-Username!");
        }
    }
}
