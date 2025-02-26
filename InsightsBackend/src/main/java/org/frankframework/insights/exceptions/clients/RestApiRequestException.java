package org.frankframework.insights.exceptions.clients;

import org.frankframework.insights.enums.ErrorCode;

public class RestApiRequestException extends ApiClientException {

    public RestApiRequestException() {
        super("Failed to communicate with server using REST requests.", ErrorCode.REST_API_REQUEST_ERROR);
    }
}
