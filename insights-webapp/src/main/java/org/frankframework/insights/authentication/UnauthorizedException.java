package org.frankframework.insights.authentication;

import org.frankframework.insights.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a user is not authenticated (401 Unauthorized)
 */
public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
