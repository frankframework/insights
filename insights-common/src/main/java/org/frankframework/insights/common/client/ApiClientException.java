package org.frankframework.insights.common.client;

import org.frankframework.insights.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ApiClientException extends ApiException {
    public ApiClientException(String message, HttpStatus status, Throwable cause) {
        super(message, status, cause);
    }
}
