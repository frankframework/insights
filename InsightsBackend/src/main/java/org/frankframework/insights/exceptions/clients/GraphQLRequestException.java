package org.frankframework.insights.exceptions.clients;

import org.frankframework.insights.enums.ErrorCode;

public class GraphQLRequestException extends ApiClientException {
    public GraphQLRequestException() {
        super("Failed to communicate with server using GraphQL requests.", ErrorCode.GRAPHQL_API_REQUEST_ERROR);
    }
}
