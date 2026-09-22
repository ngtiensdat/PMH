package com.example.paymenthub.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nhận bộ lọc từ Frontend khi người dùng bấm nút "Xuất file".
 * Tất cả field đều optional — null/blank = không lọc = xuất toàn bộ dữ liệu.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExportJobRequestDTO {

    /** Lọc theo số tài khoản (LIKE %accountNo%) */
    @Size(max = 50, message = "Số tài khoản không được vượt quá 50 ký tự")
    private String accountNo;

    /** Lọc theo mã giao dịch (LIKE %transactionCode%) */
    @Size(max = 100, message = "Mã giao dịch không được vượt quá 100 ký tự")
    private String transactionCode;

    /** Lọc theo trạng thái (EXACT: SUCCESS / FAILED) */
    @Pattern(regexp = "^(?i)(SUCCESS|FAILED)?$", message = "Trạng thái giao dịch chỉ được chọn Thành công hoặc Thất bại")
    private String status;

    /** Lọc từ ngày tạo (>=) — Linh hoạt hỗ trợ các định dạng ngày giờ */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}([ T]\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?)?(Z|[+-]\\d{2}:?\\d{2})?$", message = "Định dạng Từ ngày không hợp lệ")
    private String fromDate;

    /** Lọc đến ngày tạo (<=) — Linh hoạt hỗ trợ các định dạng ngày giờ */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}([ T]\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?)?(Z|[+-]\\d{2}:?\\d{2})?$", message = "Định dạng Đến ngày không hợp lệ")
    private String toDate;

    /** Tên trường sắp xếp (vd: createdAt, amount, status, transactionCode, accountNo) */
    private String sortBy;

    /** Hướng sắp xếp: asc hoặc desc */
    private String sortDirection;

    /**
     * Kiểm tra xem request có áp dụng bộ lọc nào không.
     * Dùng để quyết định nội dung thông báo Popup xác nhận tại frontend.
     */
    public boolean hasFilter() {
        return isNotBlank(accountNo) || isNotBlank(transactionCode)
                || isNotBlank(status) || isNotBlank(fromDate) || isNotBlank(toDate);
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
