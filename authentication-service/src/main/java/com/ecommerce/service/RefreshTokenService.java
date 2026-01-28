package com.ecommerce.service;

import com.ecommerce.entity.RefreshToken;
import com.ecommerce.repository.RefreshTokenRepository;
import com.ecommerce.util.CryptoUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@ApplicationScoped
public class RefreshTokenService {

    @Inject
    RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RefreshToken createForUser(Long userId){
        String rawToken = UUID.randomUUID().toString();
        String hashed = hashToken(rawToken);
        String encrypted = encryptToken(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.hashedToken = hashed;
        refreshToken.encryptedToken = encrypted;
        refreshToken.userId = userId;
        refreshToken.expiresAt = LocalDateTime.now().plusDays(7);
        refreshToken.persist();

        return refreshToken;
    }

    public RefreshToken findByToken(String rawToken){
        String hashed = hashToken(rawToken);
        return refreshTokenRepository.findByHashedToken(hashed);
    }

    public String getRawToken(RefreshToken refreshToken){
        return decryptToken(refreshToken.encryptedToken);
    }

    @Transactional
    public void revoke(RefreshToken refreshToken){
        RefreshToken.update("revoked = true where id = ?1", refreshToken.id);
    }

    private String encryptToken(String token) {
        return CryptoUtil.encrypt(token);
    }

    private String decryptToken(String encrypted) {
        return CryptoUtil.decrypt(encrypted);
    }

    private String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
