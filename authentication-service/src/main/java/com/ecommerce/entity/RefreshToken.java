package com.ecommerce.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends PanacheEntity {

    @Column(unique = true)
    public String hashedToken;
    public String encryptedToken;
    public Long userId;
    public LocalDateTime expiresAt;
    public boolean revoked = false;
}
