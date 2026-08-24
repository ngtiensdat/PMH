package com.example.paymenthub.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PMH_APP_USERS", indexes = {
    @Index(name = "idx_app_user_username", columnList = "USERNAME", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USERNAME", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "PASSWORD_HASH", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "FULL_NAME", length = 100)
    private String fullName;

    @Column(name = "ROLE", nullable = false, length = 50)
    private String role; // MAKER, CHECKER, ADMIN...

    @Builder.Default
    @Column(name = "FAILED_LOGIN_ATTEMPTS", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column(name = "LOCKOUT_UNTIL")
    private LocalDateTime lockoutUntil;

    @PrePersist
    protected void onCreate() {
        if (failedLoginAttempts == null) {
            failedLoginAttempts = 0;
        }
    }

    public boolean isLocked() {
        if (lockoutUntil == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(lockoutUntil);
    }
}
