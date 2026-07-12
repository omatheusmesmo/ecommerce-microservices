package com.ecommerce.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Per-IP rate limiting for /auth/login and /auth/register, the only unauthenticated
 * endpoints in the system - without this, both are open to unlimited credential
 * stuffing / account-enumeration and registration spam.
 */
public class LoginRateLimitFilter {

    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private static final Bandwidth LOGIN_LIMIT = Bandwidth.simple(10, Duration.ofMinutes(1));
    private static final Bandwidth REGISTER_LIMIT = Bandwidth.simple(5, Duration.ofMinutes(1));

    private static final Map<String, Bucket> LOGIN_BUCKETS = boundedMap();
    private static final Map<String, Bucket> REGISTER_BUCKETS = boundedMap();

    @ServerRequestFilter
    public Response filter(ContainerRequestContext requestContext, RoutingContext routingContext) {
        String path = requestContext.getUriInfo().getPath();

        Map<String, Bucket> buckets;
        Bandwidth limit;
        if (path.endsWith("auth/login")) {
            buckets = LOGIN_BUCKETS;
            limit = LOGIN_LIMIT;
        } else if (path.endsWith("auth/register")) {
            buckets = REGISTER_BUCKETS;
            limit = REGISTER_LIMIT;
        } else {
            return null;
        }

        String clientIp = clientIp(routingContext);
        Bucket bucket = buckets.computeIfAbsent(clientIp, ip -> Bucket.builder().addLimit(limit).build());

        if (bucket.tryConsume(1)) {
            return null;
        }
        return Response.status(429)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("message", "Too many requests, try again later"))
                .build();
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
