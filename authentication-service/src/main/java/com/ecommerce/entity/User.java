package com.ecommerce.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User extends PanacheEntity {

    @NotBlank
    @Email
    public String email;
    public String passwordHash;
    @Enumerated(EnumType.STRING)
    public Role role;
    public boolean active = true;
    public LocalDateTime createdAt = LocalDateTime.now();
}
