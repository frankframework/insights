package org.frankframework.webapp.common.mapper;

import org.frankframework.webapp.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class MappingException extends ApiException {
    public MappingException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_REQUEST, cause);
    }
}
