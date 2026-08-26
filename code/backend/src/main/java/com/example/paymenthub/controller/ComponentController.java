package com.example.paymenthub.controller;

import com.example.paymenthub.common.base.ApiResponse;
import com.example.paymenthub.common.base.BaseController;
import com.example.paymenthub.dto.request.ComponentDTO;
import com.example.paymenthub.dto.request.ComponentSearchCriteria;
import com.example.paymenthub.dto.response.BatchItemResultDTO;
import com.example.paymenthub.dto.response.ComponentResponseDTO;
import com.example.paymenthub.entity.ProcessingComponent;
import com.example.paymenthub.security.SecurityUtils;
import com.example.paymenthub.service.ComponentService;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/components")
@RequiredArgsConstructor
public class ComponentController extends BaseController {

    private final ComponentService service;

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('COMPONENT_VIEW')")
    public ResponseEntity<ApiResponse<Page<ComponentResponseDTO>>> search(
            @Valid @ModelAttribute ComponentSearchCriteria criteria,
            @PageableDefault(page = 0, size = 10, sort = "updatedDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Pageable pageableWithFallback = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                pageable.getSort().and(Sort.by(Sort.Direction.DESC, "componentCode"))
        );
        Page<ProcessingComponent> result = service.search(criteria, pageableWithFallback);
        Page<ComponentResponseDTO> dtoResult = result.map(ComponentResponseDTO::fromEntity);
        return ok(dtoResult, "Lấy danh sách cấu phần thành công");
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasAuthority('COMPONENT_VIEW')")
    public ResponseEntity<ApiResponse<ComponentResponseDTO>> getByCode(@PathVariable String code) {
        ProcessingComponent entity = service.getByCode(code);
        return ok(ComponentResponseDTO.fromEntity(entity), "Lấy chi tiết cấu phần thành công");
    }

    @GetMapping("/active-list")
    @PreAuthorize("hasAuthority('COMPONENT_VIEW')")
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
    @PreAuthorize("hasAuthority('COMPONENT_CREATE')")
    public ResponseEntity<ApiResponse<ComponentResponseDTO>> create(@Valid @RequestBody ComponentDTO dto) {
        String username = SecurityUtils.getCurrentUsername();
        ProcessingComponent created = service.create(dto, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ComponentResponseDTO.fromEntity(created), "Tạo mới cấu phần thành công"));
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasAuthority('COMPONENT_UPDATE')")
    public ResponseEntity<ApiResponse<ComponentResponseDTO>> update(
            @PathVariable String code,
            @Valid @RequestBody ComponentDTO dto
    ) {
        String username = SecurityUtils.getCurrentUsername();
        ProcessingComponent updated = service.update(code, dto, username);
        return ok(ComponentResponseDTO.fromEntity(updated), "Cập nhật cấu phần thành công");
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("hasAuthority('COMPONENT_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String code) {
        String username = SecurityUtils.getCurrentUsername();
        service.delete(code, username);
        return ok(null, "Xóa cấu phần thành công");
    }

    @PostMapping("/{code}/send-approval")
    @PreAuthorize("hasAuthority('COMPONENT_SEND')")
    public ResponseEntity<ApiResponse<ComponentResponseDTO>> sendApproval(@PathVariable String code) {
        String username = SecurityUtils.getCurrentUsername();
        ProcessingComponent updated = service.sendForApproval(code, username);
        return ok(ComponentResponseDTO.fromEntity(updated), "Gửi duyệt cấu phần thành công");
    }

    @PostMapping("/{code}/cancel-approval")
    @PreAuthorize("hasAuthority('COMPONENT_CANCEL')")
    public ResponseEntity<ApiResponse<ComponentResponseDTO>> cancelApproval(@PathVariable String code) {
        String username = SecurityUtils.getCurrentUsername();
        ProcessingComponent updated = service.cancelApproval(code, username);
        return ok(ComponentResponseDTO.fromEntity(updated), "Hủy duyệt cấu phần thành công");
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('COMPONENT_VIEW')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> exportData() {
        return ok(service.getRawDataForExport(), "Xuất dữ liệu thành công");
    }

    @PostMapping("/batch-approve")
    @PreAuthorize("hasAuthority('COMPONENT_APPROVE')")
    public ResponseEntity<ApiResponse<List<BatchItemResultDTO>>> batchApprove(@RequestBody List<String> codes) {
        String username = SecurityUtils.getCurrentUsername();
        return ok(service.batchApprove(codes, username), "Phê duyệt hàng loạt thành công");
    }

    @PostMapping("/batch-reject")
    @PreAuthorize("hasAuthority('COMPONENT_REJECT')")
    public ResponseEntity<ApiResponse<List<BatchItemResultDTO>>> batchReject(
            @RequestBody List<String> codes,
            @RequestParam(required = false) String reason
    ) {
        String username = SecurityUtils.getCurrentUsername();
        return ok(service.batchReject(codes, reason, username), "Từ chối/Hủy duyệt hàng loạt thành công");
    }
}
