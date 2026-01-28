package com.ecommerce.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User extends PanacheEntity {

    @NotBlank(message = "Email is required")
    @Email
    public String email;
    @NotBlank(message = "Password is required")
    public String passwordHash;
    @NotBlank(message = "Full name is required")
    public String fullName;
    @Enumerated(EnumType.STRING)
    public Role role;
    public boolean active = true;
    public LocalDateTime createdAt = LocalDateTime.now();
}
