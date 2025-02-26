package org.frankframework.insights.exceptions.clients;

import org.frankframework.insights.enums.ErrorCode;

public class UnexpectedClientException extends ApiClientException {
    public UnexpectedClientException() {
        super(
                "An unexpected error occurred while interacting with an other server",
                ErrorCode.UNEXPECTED_API_CLIENT_ERROR);
    }
}
