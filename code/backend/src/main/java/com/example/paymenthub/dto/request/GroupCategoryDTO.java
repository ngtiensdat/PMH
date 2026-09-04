package com.example.paymenthub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO nhận từ client khi tạo / cập nhật Tham số danh mục.
 * Chỉ chứa các fields người dùng có quyền gửi lên.
 * Các internal state fields (status, isActive, isDisplay, newData, version...)
 * do Server tự quản lý — không nhận từ client.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupCategoryDTO {

    @NotBlank(message = "Tên thành phần không được để trống")
    @Size(max = 255, message = "Tên thành phần tối đa 255 ký tự")
    @Pattern(regexp = "^[^\\^#|*@$`~!%&{}\\[\\]?<\"'()/\\\\:;=,]+$", message = "Tên thành phần không được chứa khoảng trắng đặc biệt hay ký tự đặc biệt")
    private String paramName;

    @NotBlank(message = "Giá trị thành phần không được để trống")
    @Size(max = 255, message = "Giá trị thành phần tối đa 255 ký tự")
    private String paramValue;

    @NotBlank(message = "Danh mục theo nhóm không được để trống")
    @Size(max = 255, message = "Danh mục theo nhóm tối đa 255 ký tự")
    private String paramType;

    @Size(max = 4000, message = "Mô tả tối đa 4000 ký tự")
    private String description;

    @NotBlank(message = "Cấu phần xử lý không được để trống")
    @Size(max = 20, message = "Cấu phần xử lý tối đa 20 ký tự")
    private String componentCode;

    @NotNull(message = "Ngày hiệu lực không được để trống")
    private LocalDateTime effectiveDate;

    private LocalDateTime endEffectiveDate;
}
