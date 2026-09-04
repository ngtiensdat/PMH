package com.example.paymenthub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO nhận từ client khi tạo / cập nhật Cấu phần xử lý.
 * Chỉ chứa các fields người dùng có quyền gửi lên.
 * Các internal state fields (status, isActive, isDisplay, newData, version...)
 * do Server tự quản lý — không nhận từ client.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComponentDTO {

    @NotBlank(message = "Mã cấu phần không được để trống")
    @Size(max = 200, message = "Mã cấu phần tối đa 200 ký tự")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Mã cấu phần chỉ gồm chữ in hoa, số và dấu gạch dưới, không chứa tiếng Việt hay ký tự đặc biệt")
    private String componentCode;

    @NotBlank(message = "Tên cấu phần không được để trống")
    @Size(max = 150, message = "Tên cấu phần tối đa 150 ký tự")
    @Pattern(regexp = "^[^\\^#|*@$`~!%&{}\\[\\]?<\"'()/\\\\:;=,]+$", message = "Tên cấu phần không được chứa khoảng trắng đặc biệt hay ký tự đặc biệt")
    private String componentName;

    @Size(max = 150, message = "Chuẩn tin điện tối đa 150 ký tự")
    private String messageType;

    @Size(max = 100, message = "Phương thức kết nối tối đa 100 ký tự")
    private String connectionMethod;

    @Pattern(regexp = "^[YN]$", message = "Kiểm tra Token chỉ nhận giá trị Y hoặc N")
    private String checkToken;

    @Size(max = 4000, message = "Mô tả tối đa 4000 ký tự")
    private String description;

    @NotNull(message = "Ngày hiệu lực không được để trống")
    private LocalDateTime effectiveDate;

    private LocalDateTime endEffectiveDate;
}
