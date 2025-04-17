package org.frankframework.insights.milestone;

import org.frankframework.insights.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class MilestoneNotFoundException extends ApiException {
    public MilestoneNotFoundException(String message, Throwable cause) {
        super(message, HttpStatus.NOT_FOUND, cause);
    }
}
