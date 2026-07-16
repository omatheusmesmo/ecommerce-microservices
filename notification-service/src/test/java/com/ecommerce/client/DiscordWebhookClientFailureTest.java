package com.ecommerce.client;

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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@QuarkusTest
@TestProfile(DiscordWebhookClientFailureTest.FailingDiscordProfile.class)
public class DiscordWebhookClientFailureTest {

    private static final int PORT = 18584;
    private static HttpServer server;

    @BeforeAll
    static void startFailingServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/webhook", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterAll
    static void stopFailingServer() {
        server.stop(0);
    }

    @Inject
    DiscordWebhookClient discordWebhookClient;

    @Test
    public void sendMessage_onErrorResponse_doesNotThrow() {
        assertDoesNotThrow(() -> discordWebhookClient.sendMessage("hello"));
    }

    @Test
    public void sendRichMessage_onErrorResponse_doesNotThrow() {
        assertDoesNotThrow(() -> discordWebhookClient.sendRichMessage(
                "Title", "Description", "red", List.of()));
    }

    public static class FailingDiscordProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "discord.webhook.url", "http://localhost:" + PORT + "/webhook",
                    "discord.webhook.enabled", "true"
            );
        }
    }
}
