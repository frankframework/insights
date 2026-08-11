package org.frankframework.insights.github.graphql;

import org.frankframework.insights.common.client.graphql.GraphQLClientException;

public class GitHubGraphQLClientException extends GraphQLClientException {
    public GitHubGraphQLClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
