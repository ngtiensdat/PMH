package com.example.paymenthub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComponentDTO {

    @NotBlank(message = "Mã cấu phần không được để trống")
    @Size(max = 200, message = "Mã cấu phần tối đa 200 ký tự")
    private String componentCode;

    @NotBlank(message = "Tên cấu phần không được để trống")
    @Size(max = 150, message = "Tên cấu phần tối đa 150 ký tự")
    private String componentName;

    @Size(max = 150, message = "Chuẩn tin điện tối đa 150 ký tự")
    private String messageType;

    @Size(max = 100, message = "Phương thức kết nối tối đa 100 ký tự")
    private String connectionMethod;

    @Pattern(regexp = "^[YN]$", message = "Kiểm tra Token chỉ nhận giá trị Y hoặc N")
    private String checkToken;

    @Size(max = 4000, message = "Mô tả tối đa 4000 ký tự")
    private String description;

    private Integer status;
    private Integer isActive;
    private Integer isDisplay;
    private String newData;

    @NotNull(message = "Ngày hiệu lực không được để trống")
    private LocalDateTime effectiveDate;

    private LocalDateTime endEffectiveDate;

    private String createdBy;
    private LocalDateTime createdDate;
    private String updatedBy;
    private LocalDateTime updatedDate;
}
