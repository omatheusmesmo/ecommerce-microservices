package com.ecommerce.repository;

import com.ecommerce.entity.RefreshToken;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;

@ApplicationScoped
public class RefreshTokenRepository extends TokenRepository<RefreshToken> {

    public RefreshToken findByHashedToken(String hashedToken) {
        return find("hashedToken = ?1 and revoked = false and expiresAt > ?2", hashedToken, LocalDateTime.now())
                .firstResult();
    }

    public long deleteExpired() {
        return delete("expiresAt < ?1 or revoked = true", LocalDateTime.now());
    }
}
