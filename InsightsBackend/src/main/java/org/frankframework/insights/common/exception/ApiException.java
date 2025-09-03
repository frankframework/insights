package org.frankframework.insights.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * The root of the custom exception hierarchy for the application.
 * This is a checked exception, forcing callers to handle potential API-level
 * errors explicitly.
 */
@Getter
public class ApiException extends Exception {
    private final HttpStatus statusCode;

    public ApiException(String message, HttpStatus statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public ApiException(String message, Throwable cause) {
        this(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
