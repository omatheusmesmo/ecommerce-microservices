package com.ecommerce.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.SequenceGenerator;

@Entity
@Table(name = "user_action_tokens")
@SequenceGenerator(
        name = "user_action_tokens_seq_gen",
        sequenceName = "user_action_tokens_seq",
        allocationSize = 50
)
public class UserActionToken extends TokenEntity {

    @Enumerated(EnumType.STRING)
    public ActionType actionType;
}
