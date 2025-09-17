package org.frankframework.insights.milestone;

import org.frankframework.insights.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class MilestoneInjectionException extends ApiException {
    public MilestoneInjectionException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
