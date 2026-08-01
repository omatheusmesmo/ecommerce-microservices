package com.ecommerce.repository;

import com.ecommerce.entity.UserActionToken;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserActionTokenRepository extends TokenRepository<UserActionToken> {}
