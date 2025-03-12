package org.frankframework.insights.exceptions.mapper;

import org.frankframework.insights.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class MappingException extends ApiException {
    public MappingException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_REQUEST, cause);
    }
}
