package com.ecommerce.health;

import com.ecommerce.client.BrevoEmailClient;
import com.ecommerce.client.DiscordWebhookClient;
import com.ecommerce.client.ReachabilityStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class DownstreamNotificationHealthCheck implements HealthCheck {

    @Inject
    DiscordWebhookClient discordWebhookClient;

    @Inject
    BrevoEmailClient brevoEmailClient;

    @Override
    public HealthCheckResponse call() {
        ReachabilityStatus discord = discordWebhookClient.checkReachability();
        ReachabilityStatus brevo = brevoEmailClient.checkReachability();

        HealthCheckResponseBuilder response = HealthCheckResponse.named("notification-downstreams")
                .withData("discord", discord.name())
                .withData("brevo", brevo.name());

        boolean healthy = discord != ReachabilityStatus.UNREACHABLE
                && brevo != ReachabilityStatus.UNREACHABLE;

        return healthy ? response.up().build() : response.down().build();
    }
}
