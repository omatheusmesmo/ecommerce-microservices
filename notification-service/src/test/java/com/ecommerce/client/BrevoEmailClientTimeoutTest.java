package com.ecommerce.client;

import com.ecommerce.dto.BrevoEmailRequest;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(BrevoEmailClientTimeoutTest.SlowBrevoProfile.class)
public class BrevoEmailClientTimeoutTest {

    private static final int PORT = 18582;
    private static HttpServer server;

    @BeforeAll
    static void startSlowServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/smtp/email", exchange -> {
            try {
                Thread.sleep(8000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterAll
    static void stopSlowServer() {
        server.stop(0);
    }

    @Inject
    BrevoEmailClient brevoEmailClient;

    @Test
    public void sendEmail_doesNotBlockBeyondConfiguredTimeout() {
        BrevoEmailRequest request = BrevoEmailRequest.builder()
                .sender("BestEcommerce", "noreply@example.com")
                .to("user@example.com", "User")
                .subject("Timeout test")
                .htmlContent("<p>hello</p>")
                .build();

        long start = System.currentTimeMillis();
        try {
            brevoEmailClient.sendEmail(request);
        } catch (Exception ignored) {
        }
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 7000, "Expected the call to be bounded by @Timeout, but took " + elapsed + "ms");
    }

    public static class SlowBrevoProfile implements QuarkusTestProfile {
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
