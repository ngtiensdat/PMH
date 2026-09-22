package com.example.paymenthub.dto.response;

import com.example.paymenthub.entity.TransactionLog;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO cho Transaction Log.
 * Không extend BaseResponseDTO vì bảng này không có Maker-Checker fields.
 *
 * Chứa static factory method fromEntity() theo đúng convention của dự án.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionLogResponseDTO {

    private Long          id;
    private String        transactionCode;
    private String        accountNo;
    private BigDecimal    amount;
    /** Trạng thái: SUCCESS / PENDING / FAILED / REVERSED */
    private String        txnStatus;
    private String        description;
    private String        referenceNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TransactionLogResponseDTO fromEntity(TransactionLog entity) {
        if (entity == null) return null;
        return TransactionLogResponseDTO.builder()
                .id(entity.getId())
                .transactionCode(entity.getTransactionCode())
                .accountNo(entity.getAccountNo())
                .amount(entity.getAmount())
                .txnStatus(entity.getStatus())
                .description(entity.getDescription())
                .referenceNo(entity.getReferenceNo())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
