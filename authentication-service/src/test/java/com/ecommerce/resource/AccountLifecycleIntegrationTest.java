package com.ecommerce.resource;

import com.ecommerce.entity.ActionType;
import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.entity.UserActionToken;
import com.ecommerce.repository.UserActionTokenRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.UserActionTokenService;
import com.ecommerce.util.CryptoUtil;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Predicate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class AccountLifecycleIntegrationTest {

    @Inject
    UserRepository userRepository;

    @Inject
    UserActionTokenRepository userActionTokenRepository;

    @Inject
    UserActionTokenService userActionTokenService;

    @Test
    void register_activate_login_refresh_fullLifecycle() {
        String ip = "198.51.100.10";
        String email = "lifecycle-" + System.nanoTime() + "@example.com";
        String password = "Passw0rd!23";

        register(ip, email, password, "Lifecycle User").statusCode(201).body("active", is(false));

        Optional<String> activationMessage = waitForKafkaMessage("authentication-email",
                value -> value.contains(email) && value.contains("\"actionType\":\"ACTIVATE\""), 10);
        assertTrue(activationMessage.isPresent(), "expected the activation event to reach Kafka");

        login(ip, email, password).statusCode(401);

        activate(rawTokenFor(email, ActionType.ACTIVATE)).statusCode(200);

        String refreshToken = login(ip, email, password).statusCode(200)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .extract().path("refreshToken");

        given()
                .contentType(ContentType.JSON)
                .body("{\"refreshToken\":\"" + refreshToken + "\"}")
                .when().post("/auth/refresh")
                .then().statusCode(200)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue());
    }

    @Test
    void requestPasswordReset_thenReset_allowsLoginWithNewPassword_andRejectsOldPassword() {
        String ip = "198.51.100.20";
        String email = "reset-" + System.nanoTime() + "@example.com";
        String oldPassword = "Passw0rd!23";
        String newPassword = "NewPassw0rd!45";

        registerAndActivate(ip, email, oldPassword);

        given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"" + email + "\"}")
                .when().post("/auth/request-password-reset")
                .then().statusCode(200);

        Optional<String> resetMessage = waitForKafkaMessage("authentication-email",
                value -> value.contains(email) && value.contains("\"actionType\":\"RESET\""), 10);
        assertTrue(resetMessage.isPresent(), "expected the password-reset event to reach Kafka");

        String resetToken = rawTokenFor(email, ActionType.RESET);

        given()
                .contentType(ContentType.JSON)
                .body("{\"token\":\"" + resetToken + "\",\"newPassword\":\"" + newPassword + "\"}")
                .when().post("/auth/reset-password")
                .then().statusCode(200);

        login(ip, email, oldPassword).statusCode(401);
        login(ip, email, newPassword).statusCode(200).body("accessToken", notNullValue());
    }

    @Test
    void promote_and_delete_requireAdminRole() {
        String adminIp = "198.51.100.30";
        String targetIp = "198.51.100.31";

        String adminEmail = "admin-" + System.nanoTime() + "@example.com";
        String adminPassword = "AdminPassw0rd!23";
        seedActiveAdmin(adminEmail, adminPassword);
        String adminToken = login(adminIp, adminEmail, adminPassword).statusCode(200).extract().path("accessToken");

        String targetEmail = "target-" + System.nanoTime() + "@example.com";
        String targetPassword = "Passw0rd!23";
        registerAndActivate(targetIp, targetEmail, targetPassword);
        String targetToken = login(targetIp, targetEmail, targetPassword).statusCode(200).extract().path("accessToken");
        Long targetId = userRepository.findByEmail(targetEmail).id;

        given()
                .header("Authorization", "Bearer " + targetToken)
                .contentType(ContentType.JSON)
                .body("\"SELLER\"")
                .when().put("/users/" + targetId + "/promote")
                .then().statusCode(403);

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("\"SELLER\"")
                .when().put("/users/" + targetId + "/promote")
                .then().statusCode(200)
                .body("role", is("SELLER"));

        given()
                .header("Authorization", "Bearer " + targetToken)
                .when().delete("/users/" + targetId)
                .then().statusCode(403);

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when().delete("/users/" + targetId)
                .then().statusCode(204);
    }

    private void registerAndActivate(String ip, String email, String password) {
        register(ip, email, password, "Test User").statusCode(201);
        activate(rawTokenFor(email, ActionType.ACTIVATE)).statusCode(200);
    }

    private void seedActiveAdmin(String email, String password) {
        QuarkusTransaction.requiringNew().run(() -> {
            User admin = new User();
            admin.email = email;
            admin.passwordHash = CryptoUtil.hashPassword(password);
            admin.fullName = "Admin User";
            admin.role = Role.ADMIN;
            admin.active = true;
            userRepository.persist(admin);
        });
    }

    private ValidatableResponse register(String ip, String email, String password, String fullName) {
        return given()
                .header("X-Forwarded-For", ip)
                .contentType(ContentType.JSON)
                .body("{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"fullName\":\"" + fullName + "\"}")
                .when().post("/auth/register")
                .then();
    }

    private ValidatableResponse login(String ip, String email, String password) {
        return given()
                .header("X-Forwarded-For", ip)
                .contentType(ContentType.JSON)
                .body("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
                .when().post("/auth/login")
                .then();
    }

    private ValidatableResponse activate(String token) {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"token\":\"" + token + "\"}")
                .when().post("/auth/activate")
                .then();
    }

    private String rawTokenFor(String email, ActionType actionType) {
        User user = userRepository.findByEmail(email);
        UserActionToken token = userActionTokenRepository
                .find("userId = ?1 and actionType = ?2", user.id, actionType)
                .firstResult();
        return userActionTokenService.getRawToken(token);
    }

    private Optional<String> waitForKafkaMessage(String topic, Predicate<String> predicate, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, ConfigProvider.getConfig().getValue("kafka.bootstrap.servers", String.class));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(topic));
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));
                for (ConsumerRecord<String, String> record : records.records(topic)) {
                    if (predicate.test(record.value())) {
                        return Optional.of(record.value());
                    }
                }
            }
        }
        return Optional.empty();
    }
}
