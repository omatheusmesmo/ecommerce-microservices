package com.ecommerce.health;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DownstreamNotificationHealthCheckTest {

    @Test
    void readiness_reportsDownstreamsAsDisabled_whenNotConfigured() {
        given().when()
                .get("/q/health/ready")
                .then()
                .body("checks.find { it.name == 'notification-downstreams' }.status", is("UP"))
                .body("checks.find { it.name == 'notification-downstreams' }.data.discord", is("DISABLED"))
                .body("checks.find { it.name == 'notification-downstreams' }.data.brevo", is("DISABLED"));
    }
}
