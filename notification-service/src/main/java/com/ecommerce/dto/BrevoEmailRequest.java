package com.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation. Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record BrevoEmailRequest(
        @NotNull(message = "Sender is required")
        @Valid
        @JsonProperty("sender")
        Sender sender,

        @NotEmpty(message = "At least one recipient is required")
        @Valid
        @JsonProperty("to")
        List<Recipient> to,

        @NotBlank(message = "Subject is required")
        @JsonProperty("subject")
        String subject,

        @JsonProperty("htmlContent")
        String htmlContent,

        @JsonProperty("textContent")
        String textContent,

        @JsonProperty("params")
        Map<String, Object> params
) {
    public record Sender(
            @NotBlank(message = "Sender name is required")
            @JsonProperty("name")
            String name,

            @NotBlank(message = "Sender email is required")
            @Email(message = "Invalid sender email format")
            @JsonProperty("email")
            String email
    ) {}

    public record Recipient(
            @NotBlank(message = "Recipient email is required")
            @Email(message = "Invalid recipient email format")
            @JsonProperty("email")
            String email,

            @JsonProperty("name")
            String name
    ) {}

    public static BrevoEmailRequestBuilder builder() {
        return new BrevoEmailRequestBuilder();
    }

    public static class BrevoEmailRequestBuilder {
        private Sender sender;
        private List<Recipient> to;
        private String subject;
        private String htmlContent;
        private String textContent;
        private Map<String, Object> params;

        public BrevoEmailRequestBuilder sender(String name, String email) {
            this.sender = new Sender(name, email);
            return this;
        }

        public BrevoEmailRequestBuilder to(String email, String name) {
            this. to = List.of(new Recipient(email, name));
            return this;
        }

        public BrevoEmailRequestBuilder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public BrevoEmailRequestBuilder htmlContent(String htmlContent) {
            this.htmlContent = htmlContent;
            return this;
        }

        public BrevoEmailRequestBuilder textContent(String textContent) {
            this.textContent = textContent;
            return this;
        }

        public BrevoEmailRequestBuilder params(Map<String, Object> params) {
            this.params = params;
            return this;
        }

        public BrevoEmailRequest build() {
            return new BrevoEmailRequest(sender, to, subject, htmlContent, textContent, params);
        }
    }
}