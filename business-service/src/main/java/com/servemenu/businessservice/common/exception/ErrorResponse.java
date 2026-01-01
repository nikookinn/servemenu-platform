package com.servemenu.businessservice.common.exception;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        boolean success,
        String message,
        String error,
        int status,
        Map<String, String> fieldErrors,
        Instant timestamp
) {
    public static ErrorResponse of(String message, String error, int status) {
        return new ErrorResponse(false, message, error, status, null, Instant.now());
    }

    public static ErrorResponse of(String message, String error, int status, Map<String, String> fieldErrors) {
        return new ErrorResponse(false, message, error, status, fieldErrors, Instant.now());
    }
}
