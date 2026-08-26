package com.example.paymenthub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "PMH_REFRESH_TOKENS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USERNAME", nullable = false, length = 50)
    private String username;

    @Column(name = "TOKEN_HASH", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "EXPIRY_DATE", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "REVOKED", nullable = false)
    private boolean revoked;

    @Column(name = "CREATED_DATE", nullable = false)
    private LocalDateTime createdDate;
}
