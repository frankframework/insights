package org.frankframework.insights.exceptions.commits;

import org.frankframework.insights.enums.ErrorCode;
import org.frankframework.insights.exceptions.ApiException;

public class CommitInjectionException extends ApiException {
    public CommitInjectionException(String message, Throwable cause) {
        super(message, ErrorCode.COMMIT_INJECTION_ERROR, cause);
    }
}
