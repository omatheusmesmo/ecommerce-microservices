package com.ecommerce.health;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(DownstreamNotificationHealthCheckDownTest.UnreachableDiscordProfile.class)
class DownstreamNotificationHealthCheckDownTest {

    @Test
    void readiness_isDown_whenAnEnabledDownstreamIsUnreachable() {
        given().when()
                .get("/q/health/ready")
                .then()
                .statusCode(503)
                .body("checks.find { it.name == 'notification-downstreams' }.status", is("DOWN"))
                .body("checks.find { it.name == 'notification-downstreams' }.data.discord", is("UNREACHABLE"));
    }

    public static class UnreachableDiscordProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "discord.webhook.enabled", "true",
                    "discord.webhook.url", "http://localhost:18586/webhook");
        }
    }
}
