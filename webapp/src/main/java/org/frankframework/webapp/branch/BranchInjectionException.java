package org.frankframework.webapp.branch;

import org.frankframework.webapp.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class BranchInjectionException extends ApiException {
    public BranchInjectionException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
