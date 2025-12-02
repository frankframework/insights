package org.frankframework.insights.ratelimit;

import org.frankframework.insights.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a user exceeds the rate limit (429 Too Many Requests)
 */
public class RateLimitExceededException extends ApiException {
    public RateLimitExceededException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS);
    }
}
