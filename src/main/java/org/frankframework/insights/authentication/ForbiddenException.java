package org.frankframework.insights.authentication;

import org.frankframework.insights.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a user is authenticated but not authorized to access a resource (403 Forbidden)
 */
public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
