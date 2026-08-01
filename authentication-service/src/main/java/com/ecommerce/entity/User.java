package com.ecommerce.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@SequenceGenerator(
        name = "users_seq_gen",
        sequenceName = "users_seq",
        allocationSize = 50
)
public class User extends PanacheEntity {

    @NotBlank(message = "Email is required")
    @Email
    @Column(unique = true, updatable = false)
    public String email;
    @NotBlank(message = "Password is required")
    public String passwordHash;
    @NotBlank(message = "Full name is required")
    public String fullName;
    @Enumerated(EnumType.STRING)
    public Role role;
    public boolean active;
    public LocalDateTime createdAt = LocalDateTime.now();
}
