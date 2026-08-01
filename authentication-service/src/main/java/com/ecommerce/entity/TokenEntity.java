package com.ecommerce.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class TokenEntity extends PanacheEntity {

    @Column(unique = true)
    public String hashedToken;

    public String encryptedToken;
    public Long userId;
    public LocalDateTime expiresAt;
}
