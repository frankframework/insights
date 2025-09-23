package org.frankframework.webapp.pullrequest;

import org.frankframework.webapp.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class PullRequestInjectionException extends ApiException {
    public PullRequestInjectionException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
