package com.ecommerce.client;

import com.ecommerce.dto.BrevoEmailRequest;
import com.ecommerce.dto.BrevoEmailResponse;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
@TestProfile(BrevoEmailClientFailureTest.FailingBrevoProfile.class)
public class BrevoEmailClientFailureTest {

    private static final int PORT = 18583;
    private static HttpServer server;

    @BeforeAll
    static void startFailingServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/smtp/email", exchange -> {
            byte[] body = "{\"message\":\"invalid api key\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(400, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
    }

    @AfterAll
    static void stopFailingServer() {
        server.stop(0);
    }

    @Inject
    BrevoEmailClient brevoEmailClient;

    private BrevoEmailRequest request() {
        return BrevoEmailRequest.builder()
                .sender("BestEcommerce", "noreply@example.com")
                .to("user@example.com", "User")
                .subject("Failure test")
                .htmlContent("<p>hello</p>")
                .build();
    }

    @Test
    public void sendEmail_onErrorResponse_returnsNullWithoutThrowing() {
        BrevoEmailResponse response = assertDoesNotThrow(() -> brevoEmailClient.sendEmail(request()));

        assertNull(response, "A non-201 Brevo response should surface as a null result, not an exception");
    }

    public static class FailingBrevoProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "brevo.api.key", "test-api-key",
                    "brevo.api.url", "http://localhost:" + PORT,
                    "notification.email.enabled", "true"
            );
        }
    }
}
