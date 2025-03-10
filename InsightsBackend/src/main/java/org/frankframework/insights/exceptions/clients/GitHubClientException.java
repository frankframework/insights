package org.frankframework.insights.exceptions.clients;

import org.frankframework.insights.enums.ErrorCode;
import org.frankframework.insights.exceptions.ApiException;

public class GitHubClientException extends ApiException {
    public GitHubClientException(String message, Throwable cause) {
        super(message, ErrorCode.GITHUB_CLIENT_ERROR, cause);
    }
}
