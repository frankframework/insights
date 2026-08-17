package org.frankframework.insights.github.rest;

import org.frankframework.insights.common.client.rest.RestClientException;

public class GitHubRestClientException extends RestClientException {
    public GitHubRestClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
