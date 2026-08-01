package com.ecommerce.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.ecommerce.dto.BrevoEmailRequest;
import com.ecommerce.dto.BrevoEmailResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(BrevoEmailClientUnreachableTest.UnreachableBrevoProfile.class)
public class BrevoEmailClientUnreachableTest {

    @Inject
    BrevoEmailClient brevoEmailClient;

    @Test
    public void sendEmail_onConnectionRefused_returnsNullWithoutThrowing() {
        BrevoEmailRequest request = BrevoEmailRequest.builder()
                .sender("BestEcommerce", "noreply@example.com")
                .to("user@example.com", "User")
                .subject("Unreachable test")
                .htmlContent("<p>hello</p>")
                .build();

        BrevoEmailResponse response = assertDoesNotThrow(() -> brevoEmailClient.sendEmail(request));

        assertNull(response, "A connection failure should surface as a null result, not an exception");
    }

    public static class UnreachableBrevoProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "brevo.api.key", "test-api-key",
                    "brevo.api.url", "http://localhost:18585",
                    "notification.email.enabled", "true");
        }
    }
}
