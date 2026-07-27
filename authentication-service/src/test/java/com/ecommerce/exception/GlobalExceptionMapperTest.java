package com.ecommerce.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkiverse.httpproblem.HttpProblem;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.Test;

class GlobalExceptionMapperTest {

    private final GlobalExceptionMapper mapper = new GlobalExceptionMapper();

    @Test
    void bulkheadException_mapsTo503() {
        HttpProblem problem = mapper.toProblem(new BulkheadException("bulkhead full"));
        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), problem.getStatusCode());
    }

    @Test
    void securityException_mapsTo401() {
        HttpProblem problem = mapper.toProblem(new SecurityException("Invalid credentials or inactive user"));
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), problem.getStatusCode());
    }

    @Test
    void unrecognizedException_mapsTo500WithoutLeakingItsMessage() {
        HttpProblem problem = mapper.toProblem(new RuntimeException("connection string user=admin"));
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), problem.getStatusCode());
        assertEquals("An unexpected error occurred. Please try again later.", problem.getDetail());
    }
}
