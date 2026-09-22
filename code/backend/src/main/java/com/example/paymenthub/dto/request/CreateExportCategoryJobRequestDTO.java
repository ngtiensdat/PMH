package com.example.paymenthub.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO nhận bộ lọc từ Frontend khi người dùng bấm "Xuất file" ở module Category.
 * Tất cả field đều optional — null/rỗng = không lọc = xuất toàn bộ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExportCategoryJobRequestDTO {

    /** Tên module — dùng để phân biệt worker xử lý; không hiển thị với người dùng */
    public static final String MODULE = "CATEGORY";

    /** Lọc theo nhóm danh mục (LIKE %paramType%) */
    @Size(max = 200)
    private String paramType;

    /** Lọc theo giá trị thành phần (LIKE %paramValue%) */
    @Size(max = 200)
    private String paramValue;

    /** Lọc theo tên thành phần (LIKE %paramName%) */
    @Size(max = 200)
    private String paramName;

    /** Lọc theo trạng thái duyệt (số nguyên: 0=NEW, 1=PENDING, 2=APPROVED, 3=REJECTED, 4=CANCELED) */
    private List<Integer> status;

    /** Lọc theo tình trạng hoạt động (1=Active, 0=Inactive) */
    private List<Integer> isActive;

    /** Tên trường sắp xếp */
    private String sortBy;

    /** Hướng sắp xếp: asc hoặc desc */
    private String sortDirection;

    /**
     * Kiểm tra xem request có áp dụng bộ lọc nào không.
     */
    public boolean hasFilter() {
        return isNotBlank(paramType) || isNotBlank(paramValue) || isNotBlank(paramName)
                || (status != null && !status.isEmpty())
                || (isActive != null && !isActive.isEmpty());
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
