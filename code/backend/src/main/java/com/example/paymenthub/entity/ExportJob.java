package com.example.paymenthub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "EXPORT_JOB_02")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", columnDefinition = "NUMBER")
    private Long id;

    @Column(name = "USER_ID", nullable = false, columnDefinition = "NUMBER")
    private Long userId;

    @Lob
    @JdbcTypeCode(SqlTypes.CLOB)
    @Column(name = "FILTER_PAYLOAD")
    private String filterPayload;

    @Column(name = "EXPORT_URL", length = 4000)
    private String exportUrl;

    @Column(name = "STATUS", length = 512)
    private String status;

    @Column(name = "FILE_NAME", length = 4000)
    private String fileName;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "PROCESSED_ROWS", columnDefinition = "NUMBER")
    @Builder.Default
    private long processedRows = 0L;

    @Column(name = "TOTAL_ROWS", columnDefinition = "NUMBER")
    private Long totalRows;

    @Column(name = "MAX_EXPORT_ID", columnDefinition = "NUMBER")
    private Long maxExportId;

    @Column(name = "ERROR_MESSAGE", length = 1000)
    private String errorMessage;

    @Column(name = "EXPIRES_AT")
    private LocalDateTime expiresAt;

    @Column(name = "LAST_HEARTBEAT_AT")
    private LocalDateTime lastHeartbeatAt;

    @Column(name = "DOWNLOAD_COUNT", columnDefinition = "NUMBER DEFAULT 0")
    @Builder.Default
    private int downloadCount = 0;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.lastHeartbeatAt == null) {
            this.lastHeartbeatAt = LocalDateTime.now();
        }
        if (this.processedRows < 0) {
            this.processedRows = 0;
        }
    }
}
