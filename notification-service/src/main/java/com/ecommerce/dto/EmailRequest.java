package com.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record EmailRequest(
        @NotBlank(message = "Recipient email is required")
        @Email(message = "Invalid email format")
        String to,

        @NotBlank(message = "Subject is required")
        String subject,

        @NotBlank(message = "HTML content is required")
        String htmlContent,

        String textContent,

        List<@Email String> cc,

        List<@Email String> bcc
) {
    public EmailRequest(String to, String subject, String htmlContent, String textContent) {
        this(to, subject, htmlContent, textContent, null, null);
    }
}