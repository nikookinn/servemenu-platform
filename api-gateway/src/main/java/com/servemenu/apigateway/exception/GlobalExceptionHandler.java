package com.servemenu.apigateway.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

/**
 * Global exception handler for API Gateway
 * Provides consistent error responses across all services
 */
@Component
public class GlobalExceptionHandler extends DefaultErrorAttributes {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Throwable error = getError(request);
        
        log.error("Gateway error occurred: {}", error.getMessage(), error);

        Map<String, Object> errorAttributes = super.getErrorAttributes(request, options);
        
        // Customize error response
        errorAttributes.put("timestamp", Instant.now().toString());
        errorAttributes.put("path", request.path());
        errorAttributes.put("method", request.method().name());
        
        // Add correlation ID if present
        String correlationId = request.headers().firstHeader("X-Correlation-ID");
        if (StringUtils.hasText(correlationId)) {
            errorAttributes.put("correlationId", correlationId);
        }

        // Handle specific exceptions
        if (error instanceof ResponseStatusException rse) {
            errorAttributes.put("status", rse.getStatusCode().value());
            errorAttributes.put("error", rse.getStatusCode().toString());
            String reason = rse.getReason();
            errorAttributes.put("message", StringUtils.hasText(reason) ? reason : "Error occurred");
        } else if (error.getClass().getName().contains("CallNotPermittedException")) {
            errorAttributes.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
            errorAttributes.put("error", "Circuit Breaker Open");
            errorAttributes.put("message", "Service is temporarily unavailable due to high error rate");
        } else if (error.getClass().getName().contains("RequestNotPermitted")) {
            errorAttributes.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
            errorAttributes.put("error", "Rate Limit Exceeded");
            errorAttributes.put("message", "Too many requests. Please try again later.");
        } else {
            errorAttributes.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            errorAttributes.put("error", "Internal Server Error");
            errorAttributes.put("message", "An unexpected error occurred");
        }

        return errorAttributes;
    }
}
