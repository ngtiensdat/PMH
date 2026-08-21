package com.example.paymenthub.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class GroupCategoryDTO {

    private Long id;

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

    private Integer status; // 1: Mới, 3: Chờ duyệt, 4: Đã duyệt, 5: Từ chối, 7: Hủy duyệt

    @Min(value = 0, message = "Trạng thái hoạt động không hợp lệ")
    @Max(value = 1, message = "Trạng thái hoạt động không hợp lệ")
    private Integer isActive; // 0: Không hoạt động, 1: Hoạt động

    private Integer isDisplay; // 1: Chưa duyệt, 2: Đã duyệt
    private String newData;

    @NotNull(message = "Ngày hiệu lực không được để trống")
    private LocalDateTime effectiveDate;

    private LocalDateTime endEffectiveDate;

    private Long version;

    private String createdBy;
    private LocalDateTime createdDate;
    private String updatedBy;
    private LocalDateTime updatedDate;
}
