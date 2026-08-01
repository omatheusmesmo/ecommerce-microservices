package com.ecommerce.service;

import com.ecommerce.entity.ActionType;
import com.ecommerce.entity.UserActionToken;
import com.ecommerce.exception.TokenExpiredException;
import com.ecommerce.repository.UserActionTokenRepository;
import com.ecommerce.util.CryptoUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class UserActionTokenService {

    @ConfigProperty(name = "app.token.activation.expiry.hours", defaultValue = "24")
    int activationExpiryHours;

    @ConfigProperty(name = "app.token.reset.expiry.minutes", defaultValue = "15")
    int resetExpiryMinutes;

    @Inject
    UserActionTokenRepository repository;

    @Transactional
    public UserActionToken createForUser(Long userId, ActionType actionType) {
        String rawToken = UUID.randomUUID().toString();
        String hashed = CryptoUtil.hashToken(rawToken);
        String encrypted = CryptoUtil.encrypt(rawToken);

        LocalDateTime expiresAt = getExpirationTime(actionType);

        UserActionToken token = new UserActionToken();
        token.hashedToken = hashed;
        token.encryptedToken = encrypted;
        token.userId = userId;
        token.expiresAt = expiresAt;
        token.actionType = actionType;
        repository.persist(token);

        return token;
    }

    private LocalDateTime getExpirationTime(ActionType actionType) {
        return switch (actionType) {
            case ACTIVATE -> LocalDateTime.now().plusHours(activationExpiryHours);
            case RESET -> LocalDateTime.now().plusMinutes(resetExpiryMinutes);
            default -> throw new IllegalArgumentException("Unsupported action type: " + actionType);
        };
    }

    public UserActionToken findByToken(String rawToken, ActionType actionType) {
        String hashed = CryptoUtil.hashToken(rawToken);
        return repository
                .find(
                        "hashedToken = ?1 and actionType = ?2 and expiresAt > ?3",
                        hashed,
                        actionType,
                        LocalDateTime.now())
                .firstResult();
    }

    public String getRawToken(UserActionToken token) {
        try {
            return CryptoUtil.decrypt(token.encryptedToken);
        } catch (Exception e) {
            throw new TokenExpiredException("Token decryption failed");
        }
    }

    @Transactional
    public void deleteToken(UserActionToken token) {
        token.delete();
    }
}
