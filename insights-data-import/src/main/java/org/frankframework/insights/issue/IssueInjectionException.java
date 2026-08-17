package org.frankframework.insights.issue;

import org.frankframework.insights.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class IssueInjectionException extends ApiException {
    public IssueInjectionException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
