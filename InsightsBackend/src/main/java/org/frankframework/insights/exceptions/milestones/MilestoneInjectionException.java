package org.frankframework.insights.exceptions.milestones;

import org.frankframework.insights.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class MilestoneInjectionException extends ApiException {
    public MilestoneInjectionException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
