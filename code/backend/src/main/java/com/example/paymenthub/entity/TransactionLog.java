package com.example.paymenthub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "TRANSACTION_LOG")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TRANSACTION_CODE", nullable = false)
    private String transactionCode;

    @Column(name = "ACCOUNT_NO", nullable = false)
    private String accountNo;

    @Column(name = "AMOUNT", nullable = false, columnDefinition = "NUMBER")
    private BigDecimal amount;

    @Column(name = "STATUS", nullable = false)
    private String status;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "REFERENCE_NO")
    private String referenceNo;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
