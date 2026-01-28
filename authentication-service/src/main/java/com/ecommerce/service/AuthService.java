package com.ecommerce.service;

import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RefreshRequest;
import com.ecommerce.dto.TokenResponse;
import com.ecommerce.entity.RefreshToken;
import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.util.CryptoUtil;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class AuthService {

    @Inject
    UserRepository userRepository;

    @Inject
    RefreshTokenService refreshTokenService;

    @Inject
    JWTParser jwtParser;

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email());
        if (user == null || !user.active || !CryptoUtil.verifyPassword(request.password(), user.passwordHash)) {
            throw new SecurityException("Invalid credentials or inactive user");
        }
        String accessToken = generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createForUser(user.id);
        String rawRefreshToken = refreshTokenService.getRawToken(refreshToken);

        return new TokenResponse(accessToken, rawRefreshToken);
    }

    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken oldRefresh = refreshTokenService.findByToken(request.refreshToken());
        if (oldRefresh == null) {
            throw new SecurityException("Invalid refresh token");
        }
        refreshTokenService.revoke(oldRefresh);

        User user = userRepository.findById(oldRefresh.userId);
        String newAccess = generateAccessToken(user);
        RefreshToken newRefresh = refreshTokenService.createForUser(user.id);
        String newRawRefresh = refreshTokenService.getRawToken(newRefresh);

        return new TokenResponse(newAccess, newRawRefresh);
    }

    private String generateAccessToken(User user) {
        Set<String> groups = Set.of(user.role.toString());
        String encryptedEmail = CryptoUtil.encrypt(user.email);
        String encryptedUserId = CryptoUtil.encrypt(user.id.toString());
        String opaqueSubject = UUID.randomUUID().toString();
        return Jwt.issuer("authentication-service")
                .subject(opaqueSubject)
                .claim("encrypted_email", encryptedEmail)
                .claim("user_id_encrypted", encryptedUserId)
                .groups(groups)
                .expiresIn(Duration.ofMinutes(15))
                .sign();
    }

    public Long getUserIdFromToken(String token) {
        try {
            var jwt = jwtParser.parse(token);
            String encryptedUserId = jwt.getClaim("user_id_encrypted");
            return encryptedUserId != null ? Long.parseLong(CryptoUtil.decrypt(encryptedUserId)) : null;
        } catch (ParseException | NumberFormatException e) {
            return null;
        }
    }
}