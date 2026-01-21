package com.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BrevoEmailResponse(
        @JsonProperty("messageId") String messageId
) {}