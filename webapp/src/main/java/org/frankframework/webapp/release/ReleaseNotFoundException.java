package org.frankframework.webapp.release;

import org.frankframework.webapp.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ReleaseNotFoundException extends ApiException {
    public ReleaseNotFoundException(String message, Throwable cause) {
        super(message, HttpStatus.NOT_FOUND, cause);
    }
}
