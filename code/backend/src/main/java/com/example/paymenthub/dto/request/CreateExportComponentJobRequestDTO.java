package com.example.paymenthub.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO nhận bộ lọc từ Frontend khi người dùng bấm "Xuất file" ở module Component.
 * Tất cả field đều optional — null/rỗng = không lọc = xuất toàn bộ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExportComponentJobRequestDTO {

    /** Tên module — dùng để phân biệt worker xử lý; không hiển thị với người dùng */
    public static final String MODULE = "COMPONENT";

    /** Lọc theo mã cấu phần (LIKE %componentCode%) */
    @Size(max = 100)
    private String componentCode;

    /** Lọc theo tên cấu phần (LIKE %componentName%) */
    @Size(max = 200)
    private String componentName;

    /** Lọc theo trạng thái duyệt (số nguyên) */
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
        return isNotBlank(componentCode) || isNotBlank(componentName)
                || (status != null && !status.isEmpty())
                || (isActive != null && !isActive.isEmpty());
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
