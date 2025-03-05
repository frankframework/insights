package org.frankframework.insights.exceptions.clients;

import org.frankframework.insights.enums.ErrorCode;
import org.frankframework.insights.exceptions.ApiException;

public class GraphQLClientException extends ApiException {
    public GraphQLClientException() {
        super("Failed to communicate with he server using the spring graphql client.", ErrorCode.GRAPHQL_CLIENT_ERROR);
    }
}
