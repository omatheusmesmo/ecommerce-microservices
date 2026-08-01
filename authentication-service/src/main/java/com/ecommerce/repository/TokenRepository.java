package com.ecommerce.repository;

import com.ecommerce.entity.TokenEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;

@ApplicationScoped
public abstract class TokenRepository<T extends TokenEntity> implements PanacheRepository<T> {

    public T findByHashedToken(String hashedToken) {
        return find("hashedToken = ?1 and expiresAt > ?2", hashedToken, LocalDateTime.now())
                .firstResult();
    }

    public long deleteExpired() {
        return delete("expiresAt < ?1", LocalDateTime.now());
    }
}
