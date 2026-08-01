package com.ecommerce.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public enum ActionType {
    ACTIVATE,
    RESET
}
