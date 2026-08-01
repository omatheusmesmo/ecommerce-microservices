package com.ecommerce.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(DiscordWebhookClientTimeoutTest.SlowDiscordProfile.class)
public class DiscordWebhookClientTimeoutTest {

    private static final int PORT = 18581;
    private static HttpServer server;

    @BeforeAll
    static void startSlowServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/webhook", exchange -> {
            try {
                Thread.sleep(8000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterAll
    static void stopSlowServer() {
        server.stop(0);
    }

    @Inject
    DiscordWebhookClient discordWebhookClient;

    @Test
    public void sendMessage_doesNotBlockBeyondConfiguredTimeout() {
        long start = System.currentTimeMillis();
        try {
            discordWebhookClient.sendMessage("hello");
        } catch (Exception ignored) {
        }
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed < 7000, "Expected the call to be bounded by @Timeout, but took " + elapsed + "ms");
    }

    public static class SlowDiscordProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "discord.webhook.url", "http://localhost:" + PORT + "/webhook", "discord.webhook.enabled", "true");
        }
    }
}
