package org.frankframework.insights.github;

import org.frankframework.insights.common.client.graphql.GraphQLClientException;

public class GitHubClientException extends GraphQLClientException {
    public GitHubClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
