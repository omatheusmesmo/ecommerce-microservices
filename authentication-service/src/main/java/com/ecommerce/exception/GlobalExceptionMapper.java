package com.ecommerce.exception;

import io.quarkiverse.httpproblem.ExceptionMapperBase;
import io.quarkiverse.httpproblem.HttpProblem;
import io.quarkiverse.httpproblem.postprocessing.PostProcessorsRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.NoSuchElementException;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;

@Provider
public class GlobalExceptionMapper extends ExceptionMapperBase<RuntimeException>
        implements ExceptionMapper<RuntimeException> {

    public GlobalExceptionMapper() {}

    @Inject
    public GlobalExceptionMapper(PostProcessorsRegistry postProcessorsRegistry) {
        super(postProcessorsRegistry);
    }

    @Override
    protected HttpProblem toProblem(RuntimeException exception) {

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

        if (exception instanceof BulkheadException) {
            return problem(
                    Response.Status.SERVICE_UNAVAILABLE,
                    "Service Unavailable",
                    "The server is under heavy load. Please retry shortly.");
        }

        if (exception instanceof SecurityException) {
            return problem(
                    Response.Status.UNAUTHORIZED,
                    "Authentication Failed",
                    detailOr(exception, "Authentication or authorization failed."));
        }

        return problem(
                Response.Status.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.");
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
