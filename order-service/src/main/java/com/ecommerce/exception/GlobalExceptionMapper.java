package com.ecommerce.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {

        if (exception instanceof WebApplicationException webEx) {
            LOG.warnf("WebApplicationException:  %s", exception.getMessage());
            return webEx.getResponse();
        }

        if (exception instanceof NoSuchElementException) {
            LOG.warnf("Resource not found: %s", exception. getMessage());
            return buildErrorResponse(
                    Response.Status. NOT_FOUND,
                    "Resource Not Found",
                    exception.getMessage() != null ? exception.getMessage() : "The requested resource was not found"
            );
        }

        if (exception instanceof IllegalArgumentException) {
            LOG.warnf("Bad request: %s", exception.getMessage());
            return buildErrorResponse(
                    Response.Status.BAD_REQUEST,
                    "Bad Request",
                    exception.getMessage()
            );
        }

        if (exception instanceof IllegalStateException) {
            LOG.warnf("Invalid operation: %s", exception. getMessage());
            return buildErrorResponse(
                    Response.Status.BAD_REQUEST,
                    "Invalid Operation",
                    exception.getMessage()
            );
        }

        LOG.errorf(exception, "Unexpected error occurred");
        return buildErrorResponse(
                Response.Status.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred.  Please try again later."
        );
    }

    private Response buildErrorResponse(Response.Status status, String error, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status. getStatusCode());

        return Response
                .status(status)
                .entity(errorResponse)
                .build();
    }
}