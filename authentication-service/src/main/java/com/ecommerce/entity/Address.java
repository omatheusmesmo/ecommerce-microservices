package com.ecommerce.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "addresses")
@SequenceGenerator(name = "addresses_seq_gen", sequenceName = "addresses_seq", allocationSize = 50)
public class Address extends PanacheEntity {

    @Column(nullable = false, updatable = false)
    public Long userId;

    @Column(length = 50)
    public String label;

    @NotBlank(message = "Street is required")
    @Column(nullable = false)
    public String street;

    @NotBlank(message = "Number is required")
    @Column(nullable = false, length = 20)
    public String number;

    public String complement;

    @NotBlank(message = "City is required")
    @Column(nullable = false, length = 100)
    public String city;

    @NotBlank(message = "State is required")
    @Column(nullable = false, length = 100)
    public String state;

    @NotBlank(message = "Zip code is required")
    @Column(nullable = false, length = 20)
    public String zipCode;

    @NotBlank(message = "Country is required")
    @Column(nullable = false, length = 100)
    public String country;

    @Column(nullable = false)
    public boolean isDefault;

    @Column(nullable = false, updatable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
}
