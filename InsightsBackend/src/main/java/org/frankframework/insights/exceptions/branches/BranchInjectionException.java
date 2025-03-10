package org.frankframework.insights.exceptions.branches;

import org.frankframework.insights.enums.ErrorCode;
import org.frankframework.insights.exceptions.ApiException;

public class BranchInjectionException extends ApiException {
    public BranchInjectionException(String message, Throwable cause) {
        super(message, ErrorCode.BRANCH_INJECTION_ERROR, cause);
    }
}
