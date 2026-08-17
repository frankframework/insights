package org.frankframework.insights.issueprojects;

import org.frankframework.insights.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class IssueProjectItemInjectionException extends ApiException {
    public IssueProjectItemInjectionException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
