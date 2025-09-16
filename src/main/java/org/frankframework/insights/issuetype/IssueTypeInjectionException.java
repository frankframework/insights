package org.frankframework.insights.issuetype;

import org.frankframework.insights.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class IssueTypeInjectionException extends ApiException {
    public IssueTypeInjectionException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
