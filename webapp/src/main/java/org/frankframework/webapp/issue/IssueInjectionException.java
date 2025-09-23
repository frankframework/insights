package org.frankframework.webapp.issue;

import org.frankframework.webapp.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class IssueInjectionException extends ApiException {
    public IssueInjectionException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
