package org.frankframework.insights.exceptions.milestones;

import org.frankframework.insights.enums.ErrorCode;
import org.frankframework.insights.exceptions.ApiException;

public class MilestoneInjectionException extends ApiException {
    public MilestoneInjectionException(String message, Throwable cause) {
        super(message, ErrorCode.MILESTONE_INJECTION_ERROR, cause);
    }
}
