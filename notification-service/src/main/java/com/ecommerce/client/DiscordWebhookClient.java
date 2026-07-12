package com.ecommerce.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class DiscordWebhookClient {

    private final static Logger LOG = Logger.getLogger(DiscordWebhookClient.class);

    @ConfigProperty(name = "discord.webhook.url")
    Optional<String> webhookUrl;
    @ConfigProperty(name = "discord.webhook.enabled", defaultValue = "true")
    boolean enabled;

    private final Client client = ClientBuilder.newClient();

    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 10, delayUnit = ChronoUnit.SECONDS)
    public void sendMessage(String content){
        if (!isDiscordReady()){
            return;
        }

        try{
            Map<String, Object> payload = Map.of("content",content);

            Response response = client.target(webhookUrl.get())
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(payload));

            if(response.getStatus()==204){
                LOG.debugf("Discord message sent: %s", truncate(content, 50));
            }else {
                LOG.warnf("Discord returned status %d", response.getStatus());
            }

            response.close();
        }catch (Exception e){
            LOG.errorf(e, "Failed to send Discord message");
        }
    }

    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 10, delayUnit = ChronoUnit.SECONDS)
    public void sendRichMessage(String title, String description, String color, List<Field> fields){
        if (!isDiscordReady()){
            return;
        }

        try{
            LOG.debugf("Sending Discord rich message: title=%s, color=%s, fields=%d", title, color, fields.size());

            Map<String, Object> embed = Map.of(
                    "title", title,
                    "description", description,
                    "color", parseColor(color),
                    "fields", fields.stream().map(f -> Map.of(
                            "name", f.name(),
                            "value", f.value(),
                            "inline", f.inline()
                    )).toList(),
                    "timestamp", Instant.now().toString()
            );

            Map<String, Object> payload = Map.of("embeds", List.of(embed));

            Response response = client.target(webhookUrl.get())
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(payload));

            int status = response.getStatus();
            if (status == 204) {
                LOG.infof("Discord rich message sent successfully: %s", title);
            } else {
                String errorBody = response.readEntity(String.class);
                LOG.errorf("Discord API returned error status %d: %s", status, errorBody);
            }

            response.close();
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send Discord rich message: %s - error: %s", title, e.getMessage());
        }
    }

    private boolean isDiscordReady(){
        if (!isConfigured()){
            LOG.debug("Discord webhook not configured");
            return false;
        }

        if (!enabled){
            LOG.debug("Discord webhook is disabled");
            return false;
        }

        return true;
    }

    public boolean isConfigured() {
        return !webhookUrl.get().isBlank();
    }

    private int parseColor(String color) {
        return switch (color.toLowerCase()) {
            case "green" -> 0x2ECC71;
            case "red" -> 0xE74C3C;
            case "yellow" -> 0xF1C40F;
            case "blue" -> 0x3498DB;
            case "orange" -> 0xE67E22;
            case "purple" -> 0x9B59B6;
            default -> 0x7289DA;
        };
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }

    public record Field(String name, String value, boolean inline) {}
}
