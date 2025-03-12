package org.frankframework.insights.exceptions.clients;

import org.frankframework.insights.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class GitHubClientException extends ApiException {
    public GitHubClientException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
