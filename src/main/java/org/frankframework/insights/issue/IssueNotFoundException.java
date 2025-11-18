package org.frankframework.insights.issue;

import org.frankframework.insights.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class IssueNotFoundException extends ApiException {
    public IssueNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
