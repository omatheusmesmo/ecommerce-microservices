package com.ecommerce.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_tokens")
@SequenceGenerator(name = "refresh_tokens_seq_gen", sequenceName = "refresh_tokens_seq", allocationSize = 50)
public class RefreshToken extends TokenEntity {

    public boolean revoked = false;
}
