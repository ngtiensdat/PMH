package com.example.paymenthub.entity;

import com.example.paymenthub.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PMH_GROUP_CATEGORY", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"PARAM_NAME", "PARAM_VALUE", "PARAM_TYPE"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "PARAM_NAME", nullable = false, length = 255)
    private String paramName;

    @Column(name = "PARAM_VALUE", nullable = false, length = 255)
    private String paramValue;

    @Column(name = "PARAM_TYPE", nullable = false, length = 255)
    private String paramType;

    @Column(name = "DESCRIPTION", length = 4000)
    private String description;

    @Column(name = "COMPONENT_CODE", nullable = false, length = 255)
    private String componentCode;

    @Column(name = "STATUS", nullable = false)
    private Integer status; // 1: Mới, 3: Chờ duyệt, 4: Đã duyệt, 5: Từ chối, 7: Hủy duyệt

    @Column(name = "IS_ACTIVE", nullable = false)
    private Integer isActive; // 0: Không hoạt động, 1: Hoạt động

    @Column(name = "IS_DISPLAY", nullable = false)
    private Integer isDisplay; // 1: Chưa duyệt (cho phép xóa), 2: Đã duyệt (không cho phép xóa)

    @Column(name = "NEW_DATA", length = 4000)
    private String newData; // Lưu JSON chuỗi dữ liệu mới thay đổi trước khi duyệt

    @Column(name = "EFFECTIVE_DATE", nullable = false)
    private LocalDateTime effectiveDate;

    @Column(name = "END_EFFECTIVE_DATE")
    private LocalDateTime endEffectiveDate;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (isActive == null) isActive = 1;
        if (isDisplay == null) isDisplay = 1;
        if (status == null) status = 1; // Mới
    }

    @PreUpdate
    @Override
    protected void onUpdate() {
        super.onUpdate();
    }
}
