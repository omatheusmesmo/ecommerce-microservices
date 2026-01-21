package com.ecommerce.client;

import com.ecommerce.dto.BrevoEmailRequest;
import com.ecommerce.dto.BrevoEmailResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Optional;

@ApplicationScoped
public class BrevoEmailClient {

    private static final Logger LOG = Logger.getLogger(BrevoEmailClient.class);

    @ConfigProperty(name = "brevo.api.key")
    Optional<String> apiKey;

    @ConfigProperty(name = "brevo.api.url", defaultValue = "https://api.brevo.com/v3")
    String apiUrl;

    @ConfigProperty(name = "notification.email.enabled", defaultValue = "false")
    boolean enabled;

    private final Client client = ClientBuilder.newClient();

    public BrevoEmailResponse sendEmail(BrevoEmailRequest request){
     if (!isConfigured()){
         LOG.debug("Brevo API not configured, skipping email");
         return null;
     }
     if (!enabled){
         LOG.debug("Email notifications are disabled, skipping email");
         return null;
     }

     try {
         LOG.infof("Sending e-mail to %s: %s",
                 request.to().getFirst().email(), request.subject());

         Response response = client.target(apiUrl + "/smtp/email")
                 .request(MediaType.APPLICATION_JSON)
                 .header("api-key", apiKey.get())
                 .header("Content-Type", "application/json")
                 .header("Accept", "application/json")
                 .post(Entity.json(request));

         int status = response.getStatus();

         if (status ==201){
             BrevoEmailResponse result = response.readEntity(BrevoEmailResponse.class);
             LOG.infof("E-mail sent successfully! MessageId: %s", result.messageId());
             response.close();
             return result;
         }else {
             String errorBody = response.readEntity(String.class);
             LOG.errorf("Brevo API error (status %d): %s", status, errorBody);
             response.close();
             return null;
         }
     }catch (Exception e){
         LOG.errorf(e, "Failed to send e-mail via Brevo");
         return null;
     }
    }

    public boolean isConfigured() {
        return !apiKey.get().isBlank();
    }
}
