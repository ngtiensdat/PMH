package com.example.paymenthub.repository;

import com.example.paymenthub.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.username = :username")
    void revokeAllByUsername(String username);

    void deleteByUsername(String username);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiryDate < :now OR r.revoked = true")
    int deleteExpiredOrRevokedTokens(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);
}
