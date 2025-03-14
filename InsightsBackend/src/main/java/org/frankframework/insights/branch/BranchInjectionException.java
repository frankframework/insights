package org.frankframework.insights.branch;

import org.springframework.http.HttpStatus;

import org.frankframework.insights.common.exception.ApiException;

public class BranchInjectionException extends ApiException {
    public BranchInjectionException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
