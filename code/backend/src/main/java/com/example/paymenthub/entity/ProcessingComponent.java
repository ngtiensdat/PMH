package com.example.paymenthub.entity;

import com.example.paymenthub.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PMH_COMPONENTS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessingComponent extends BaseEntity {

    @Id
    @Column(name = "COMPONENT_CODE", nullable = false, length = 50)
    private String componentCode;

    @Column(name = "COMPONENT_NAME", nullable = false, length = 255)
    private String componentName;

    @Column(name = "MESSAGE_TYPE", length = 100)
    private String messageType;

    @Column(name = "CONNECTION_METHOD", length = 100)
    private String connectionMethod;

    @Column(name = "CHECK_TOKEN", length = 1)
    private String checkToken; // Y: Có kiểm tra token, N: Không

    @Column(name = "DESCRIPTION", length = 4000)
    private String description;

    @Column(name = "STATUS", nullable = false)
    private Integer status; // 1: Mới, 3: Chờ duyệt, 4: Đã duyệt, 5: Từ chối, 7: Hủy duyệt

    @Column(name = "IS_ACTIVE", nullable = false)
    private Integer isActive;

    @Column(name = "IS_DISPLAY", nullable = false)
    private Integer isDisplay;

    @Column(name = "NEW_DATA", length = 4000)
    private String newData;

    @Column(name = "EFFECTIVE_DATE", nullable = false)
    private LocalDateTime effectiveDate;

    @Column(name = "END_EFFECTIVE_DATE")
    private LocalDateTime endEffectiveDate;

    @Version
    @Column(name = "VERSION")
    private Long version;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (isActive == null) isActive = 1;
        if (isDisplay == null) isDisplay = 1;
        if (status == null) status = 1;
        if (checkToken == null) checkToken = "N";
    }

    @PreUpdate
    @Override
    protected void onUpdate() {
        super.onUpdate();
    }
}
