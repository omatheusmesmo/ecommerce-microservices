package com.ecommerce.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkiverse.httpproblem.HttpProblem;
import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Test;

class GlobalExceptionMapperTest {

    private final GlobalExceptionMapper mapper = new GlobalExceptionMapper();

    @Test
    void timeoutException_mapsTo503() {
        HttpProblem problem = mapper.toProblem(new TimeoutException("timed out"));
        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), problem.getStatusCode());
    }

    @Test
    void optimisticLockException_mapsTo409() {
        HttpProblem problem = mapper.toProblem(new OptimisticLockException("stale order"));
        assertEquals(Response.Status.CONFLICT.getStatusCode(), problem.getStatusCode());
    }

    @Test
    void wrappedStaleStateException_mapsTo409() {
        HttpProblem problem = mapper.toProblem(
                new RuntimeException("commit failed", new StaleObjectStateException("Order", 1L)));
        assertEquals(Response.Status.CONFLICT.getStatusCode(), problem.getStatusCode());
    }

    @Test
    void unrecognizedException_mapsTo500WithoutLeakingItsMessage() {
        HttpProblem problem = mapper.toProblem(new RuntimeException("connection string user=admin"));
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), problem.getStatusCode());
        assertEquals("An unexpected error occurred. Please try again later.", problem.getDetail());
    }
}
