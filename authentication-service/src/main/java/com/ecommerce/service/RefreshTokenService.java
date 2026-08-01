package com.ecommerce.service;

import com.ecommerce.entity.RefreshToken;
import com.ecommerce.repository.RefreshTokenRepository;
import com.ecommerce.util.CryptoUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class RefreshTokenService {

    @Inject
    RefreshTokenRepository repository;

    @Transactional
    public RefreshToken createForUser(Long userId) {
        String rawToken = UUID.randomUUID().toString();
        String hashed = CryptoUtil.hashToken(rawToken);
        String encrypted = CryptoUtil.encrypt(rawToken);

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        RefreshToken token = new RefreshToken();
        token.hashedToken = hashed;
        token.encryptedToken = encrypted;
        token.userId = userId;
        token.expiresAt = expiresAt;
        token.revoked = false;
        repository.persist(token);

        return token;
    }

    public RefreshToken findByToken(String rawToken) {
        String hashed = CryptoUtil.hashToken(rawToken);
        return repository
                .find("hashedToken = ?1 and revoked = false and expiresAt > ?2", hashed, LocalDateTime.now())
                .firstResult();
    }

    public String getRawToken(RefreshToken token) {
        return CryptoUtil.decrypt(token.encryptedToken);
    }

    @Transactional
    public void deleteToken(RefreshToken token) {
        token.delete();
    }

    @Transactional
    public void revoke(RefreshToken token) {
        RefreshToken.update("revoked = true where id = ?1", token.id);
    }
}
