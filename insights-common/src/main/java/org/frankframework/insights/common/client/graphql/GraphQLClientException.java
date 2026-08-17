package org.frankframework.insights.common.client.graphql;

import org.frankframework.insights.common.client.ApiClientException;
import org.springframework.http.HttpStatus;

public class GraphQLClientException extends ApiClientException {
    public GraphQLClientException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
