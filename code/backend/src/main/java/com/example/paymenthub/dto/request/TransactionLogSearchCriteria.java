package com.example.paymenthub.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Search criteria cho Transaction Log.
 * Tất cả field đều optional — null/blank = không lọc theo field đó.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionLogSearchCriteria {

    /** Lọc theo số tài khoản (LIKE %accountNo%) */
    @Size(max = 50, message = "Số tài khoản không được vượt quá 50 ký tự")
    private String accountNo;

    /** Lọc theo mã giao dịch (LIKE %transactionCode%) */
    @Size(max = 100, message = "Mã giao dịch không được vượt quá 100 ký tự")
    private String transactionCode;

    /** Lọc theo trạng thái giao dịch (EXACT match: SUCCESS / FAILED) */
    @Pattern(regexp = "^(?i)(SUCCESS|FAILED)?$", message = "Trạng thái giao dịch chỉ được chọn Thành công hoặc Thất bại")
    private String status;

    /** Lọc từ ngày tạo (>=) — Linh hoạt hỗ trợ các định dạng ngày giờ */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}([ T]\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?)?(Z|[+-]\\d{2}:?\\d{2})?$", message = "Định dạng Từ ngày không hợp lệ")
    private String fromDate;

    /** Lọc đến ngày tạo (<=) — Linh hoạt hỗ trợ các định dạng ngày giờ */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}([ T]\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?)?(Z|[+-]\\d{2}:?\\d{2})?$", message = "Định dạng Đến ngày không hợp lệ")
    private String toDate;
}
