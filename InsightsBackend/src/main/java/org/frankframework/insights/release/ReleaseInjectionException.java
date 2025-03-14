package org.frankframework.insights.release;

import org.springframework.http.HttpStatus;

import org.frankframework.insights.common.exception.ApiException;

public class ReleaseInjectionException extends ApiException {
    public ReleaseInjectionException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
