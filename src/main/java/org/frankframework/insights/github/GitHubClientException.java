package org.frankframework.insights.github;

import org.frankframework.insights.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class GitHubClientException extends ApiException {
    public GitHubClientException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
