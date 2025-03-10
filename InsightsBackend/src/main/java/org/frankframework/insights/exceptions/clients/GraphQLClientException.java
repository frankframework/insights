package org.frankframework.insights.exceptions.clients;

import org.frankframework.insights.enums.ErrorCode;
import org.frankframework.insights.exceptions.ApiException;

public class GraphQLClientException extends ApiException {
    public GraphQLClientException(String message, Throwable cause) {
        super(message, ErrorCode.GRAPHQL_CLIENT_ERROR, cause);
    }
}
