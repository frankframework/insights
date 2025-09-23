package org.frankframework.webapp.issuePriority;

import org.frankframework.webapp.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class IssuePriorityInjectionException extends ApiException {
    public IssuePriorityInjectionException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
