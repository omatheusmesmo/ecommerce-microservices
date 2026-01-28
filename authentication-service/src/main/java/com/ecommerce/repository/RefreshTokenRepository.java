package com.ecommerce.repository;

import com.ecommerce.entity.RefreshToken;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;

import java.time.LocalDateTime;

@ApplicationScoped
public class RefreshTokenRepository implements PanacheRepository<RefreshToken> {

    private final ValueCommands<String, String> redis;

    public RefreshTokenRepository(RedisDataSource redisDS) {
        this.redis = redisDS.value(String.class);
    }

    public RefreshToken findByHashedToken(String hashedToken) {
        String cached = redis.get(hashedToken);
        if (cached != null) {
            return RefreshToken.find("hashedToken = ?1 and revoked = false and expiresAt > ?2", hashedToken, java.time.LocalDateTime.now()).firstResult();
        }
        RefreshToken rt = RefreshToken.find("hashedToken = ?1 and revoked = false and expiresAt > ?2", hashedToken, java.time.LocalDateTime.now()).firstResult();
        if (rt != null) {
            redis.setex(hashedToken, 3600, "valid");
        }
        return rt;
    }

    public long deleteExpired() {
        return delete("expiresAt < ?1 or revoked = true", LocalDateTime.now());
    }
}