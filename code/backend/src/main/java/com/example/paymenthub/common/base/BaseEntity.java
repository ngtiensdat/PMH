package com.example.paymenthub.common.base;

import com.example.paymenthub.common.enums.DisplayStatus;
import com.example.paymenthub.common.enums.ParamStatus;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEntity {

    @Column(name = "STATUS", nullable = false)
    private Integer status; // 1: Mới, 3: Chờ duyệt, 4: Đã duyệt, 5: Từ chối, 7: Hủy duyệt

    @Column(name = "IS_ACTIVE", nullable = false)
    private Integer isActive; // 0: Không hoạt động, 1: Hoạt động

    @Column(name = "IS_DISPLAY", nullable = false)
    private Integer isDisplay; // 1: Chưa từng duyệt, 2: Đã từng duyệt

    @Column(name = "NEW_DATA", length = 4000)
    private String newData; // Lưu JSON chuỗi dữ liệu mới thay đổi trước khi duyệt

    @Column(name = "EFFECTIVE_DATE", nullable = false)
    private LocalDateTime effectiveDate;

    @Column(name = "END_EFFECTIVE_DATE")
    private LocalDateTime endEffectiveDate;

    @Version
    @Column(name = "VERSION")
    private Long version;

    @Column(name = "CREATED_BY", length = 50, updatable = false)
    private String createdBy;

    @Column(name = "CREATED_DATE", updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "UPDATED_BY", length = 50)
    private String updatedBy;

    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        updatedDate = LocalDateTime.now();
        if (status == null) status = ParamStatus.NEW.getCode();
        if (isDisplay == null) isDisplay = DisplayStatus.INITIAL.getCode();
        updateActiveStatus();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
        updateActiveStatus();
    }

    public void updateActiveStatus() {
        if (effectiveDate == null) {
            this.isActive = 0;
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(effectiveDate) || (endEffectiveDate != null && now.isAfter(endEffectiveDate))) {
            this.isActive = 0;
        } else {
            this.isActive = 1;
        }
    }

    // ─── Domain Status & Display Helpers (Scalable across 100s of modules) ───

    public boolean isNew() {
        return Integer.valueOf(ParamStatus.NEW.getCode()).equals(this.status);
    }

    public boolean isPending() {
        return Integer.valueOf(ParamStatus.PENDING.getCode()).equals(this.status);
    }

    public boolean isApproved() {
        return Integer.valueOf(ParamStatus.APPROVED.getCode()).equals(this.status);
    }

    public boolean isRejected() {
        return Integer.valueOf(ParamStatus.REJECTED.getCode()).equals(this.status);
    }

    public boolean isCanceled() {
        return Integer.valueOf(ParamStatus.CANCELED.getCode()).equals(this.status);
    }

    public boolean isCanBeSubmitted() {
        return isNew() || isRejected() || isCanceled();
    }

    public boolean isCanBeEdited() {
        return !isPending();
    }

    public boolean isOnceApproved() {
        return Integer.valueOf(DisplayStatus.ONCE_APPROVED.getCode()).equals(this.isDisplay);
    }
}
