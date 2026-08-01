package com.ecommerce.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.quarkiverse.httpproblem.HttpProblem;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/**
 * Per-IP rate limiting for the public, unbounded product read endpoints
 * (GET /products, /products/active, /products/category/{categoryId}) - without this
 * they are open to unlimited catalog scraping, each call returning a full result page.
 */
@ApplicationScoped
public class ProductReadRateLimitFilter {

    private static final int MAX_TRACKED_CLIENTS = 10_000;

    @ConfigProperty(name = "product.ratelimit.reads-per-minute", defaultValue = "60")
    int readsPerMinute;

    private final Map<String, Bucket> buckets = boundedMap();

    @ServerRequestFilter
    public Response filter(ContainerRequestContext requestContext, RoutingContext routingContext) {
        if (!"GET".equals(requestContext.getMethod())
                || !isListEndpoint(requestContext.getUriInfo().getPath())) {
            return null;
        }

        String clientIp = clientIp(routingContext);
        Bucket bucket = buckets.computeIfAbsent(
                clientIp,
                ip -> Bucket.builder()
                        .addLimit(Bandwidth.simple(readsPerMinute, Duration.ofMinutes(1)))
                        .build());

        if (bucket.tryConsume(1)) {
            return null;
        }
        throw HttpProblem.builder()
                .withStatus(Response.Status.TOO_MANY_REQUESTS)
                .withTitle("Too Many Requests")
                .withDetail("Too many requests, try again later.")
                .build();
    }

    private boolean isListEndpoint(String path) {
        String p = path.startsWith("/") ? path.substring(1) : path;
        return p.equals("products") || p.equals("products/active") || p.startsWith("products/category/");
    }

    private String clientIp(RoutingContext routingContext) {
        String forwardedFor = routingContext.request().getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return routingContext.request().remoteAddress().host();
    }

    private static Map<String, Bucket> boundedMap() {
        return Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Entry<String, Bucket> eldest) {
                return size() > MAX_TRACKED_CLIENTS;
            }
        });
    }
}
