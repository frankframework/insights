package org.frankframework.insights.common.client.rest;

import org.frankframework.insights.common.client.ApiClientException;
import org.springframework.http.HttpStatus;

public class RestClientException extends ApiClientException {
    public RestClientException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
