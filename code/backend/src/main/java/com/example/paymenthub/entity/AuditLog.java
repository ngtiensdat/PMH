package com.example.paymenthub.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PMH_AUDIT_LOG")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    /** GROUP_CATEGORY | COMPONENT */
    @Column(name = "MODULE", nullable = false, length = 50)
    private String module;

    /** ID bản ghi (Long→String hoặc COMPONENT_CODE) */
    @Column(name = "RECORD_ID", nullable = false, length = 100)
    private String recordId;

    /** Tạo mới | Cập nhật | Xóa | Gửi duyệt | Phê duyệt | Từ chối | Hủy duyệt */
    @Column(name = "ACTION", nullable = false, length = 100)
    private String action;

    @Column(name = "PERFORMED_BY", nullable = false, length = 100)
    private String performedBy;

    @Column(name = "ACTION_DATE", nullable = false)
    private LocalDateTime actionDate;

    @Lob
    @Column(name = "OLD_DATA")
    private String oldData;       // JSON entity trước khi thay đổi

    @Lob
    @Column(name = "NEW_DATA_LOG")
    private String newDataLog;    // JSON entity sau khi thay đổi

    @Column(name = "DESCRIPTION", length = 4000)
    private String description;

    @Column(name = "STATUS_BEFORE")
    private Integer statusBefore;

    @Column(name = "STATUS_AFTER")
    private Integer statusAfter;

    @Column(name = "IP_ADDRESS", length = 100)
    private String ipAddress;

    @PrePersist
    protected void onCreate() {
        if (actionDate == null) actionDate = LocalDateTime.now();
    }
}
