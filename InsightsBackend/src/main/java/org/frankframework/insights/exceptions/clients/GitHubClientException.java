package org.frankframework.insights.exceptions.clients;

import org.frankframework.insights.enums.ErrorCode;
import org.frankframework.insights.exceptions.ApiException;

public class GitHubClientException extends ApiException {
    public GitHubClientException() {
        super("Failed to send request to GitHub using the graphql client.", ErrorCode.GITHUB_CLIENT_ERROR);
    }
}
