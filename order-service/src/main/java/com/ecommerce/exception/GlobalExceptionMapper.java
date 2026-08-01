package com.ecommerce.exception;

import io.quarkiverse.httpproblem.ExceptionMapperBase;
import io.quarkiverse.httpproblem.HttpProblem;
import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.NoSuchElementException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.hibernate.StaleStateException;

@Provider
public class GlobalExceptionMapper extends ExceptionMapperBase<RuntimeException>
        implements ExceptionMapper<RuntimeException> {

    @Override
    protected HttpProblem toProblem(RuntimeException exception) {

        if (isOptimisticLock(exception)) {
            return problem(
                    Response.Status.CONFLICT,
                    "Conflict",
                    "The resource was modified concurrently. Please reload it and retry.");
        }

        if (exception instanceof NoSuchElementException) {
            return problem(
                    Response.Status.NOT_FOUND,
                    "Resource Not Found",
                    detailOr(exception, "The requested resource was not found."));
        }

        if (exception instanceof IllegalArgumentException) {
            return problem(
                    Response.Status.BAD_REQUEST,
                    "Bad Request",
                    detailOr(exception, "The request could not be processed."));
        }

        if (exception instanceof IllegalStateException) {
            return problem(
                    Response.Status.BAD_REQUEST,
                    "Invalid Operation",
                    detailOr(exception, "The requested operation is not valid in the current state."));
        }

        if (exception instanceof TimeoutException) {
            return problem(
                    Response.Status.SERVICE_UNAVAILABLE,
                    "Service Unavailable",
                    "The request timed out. Please retry shortly.");
        }

        return problem(
                Response.Status.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.");
    }

    private boolean isOptimisticLock(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof OptimisticLockException || cause instanceof StaleStateException) {
                return true;
            }
        }
        return false;
    }

    private HttpProblem problem(Response.Status status, String title, String detail) {
        return HttpProblem.builder()
                .withStatus(status)
                .withTitle(title)
                .withDetail(detail)
                .build();
    }

    private String detailOr(RuntimeException exception, String fallback) {
        String message = exception.getMessage();
        return message != null && !message.isBlank() ? message : fallback;
    }
}
