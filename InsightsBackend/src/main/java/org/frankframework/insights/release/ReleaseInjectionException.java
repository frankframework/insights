package org.frankframework.insights.release;

import org.frankframework.insights.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ReleaseInjectionException extends ApiException {
    public ReleaseInjectionException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
