package org.frankframework.insights.exceptions.releases;

import org.frankframework.insights.enums.ErrorCode;
import org.frankframework.insights.exceptions.ApiException;

public class ReleaseInjectionException extends ApiException {
    public ReleaseInjectionException(String message, Throwable cause) {
        super(message, ErrorCode.RELEASE_INJECTION_ERROR, cause);
    }
}
