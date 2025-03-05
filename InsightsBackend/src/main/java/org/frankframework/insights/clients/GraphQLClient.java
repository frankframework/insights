package org.frankframework.insights.clients;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public abstract class GraphQLClient {
    protected final String baseUrl;
    protected final String secret;

    public GraphQLClient(String baseUrl, String secret) {
        this.baseUrl = baseUrl;
        this.secret = secret;
    }

    protected HttpGraphQlClient getGraphQLClient() {
        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + secret)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();

        return HttpGraphQlClient.builder(webClient).build();
    }

    protected abstract <T> Mono<T> sendGraphQLRequest(
            JsonNode query,
            ParameterizedTypeReference<T> responseType,
            HttpGraphQlClient graphQLClient,
            String retrievePath);

    public <T> T request(JsonNode query, ParameterizedTypeReference<T> responseType, String retrievePath) {
        try {
            HttpGraphQlClient graphQLClient = getGraphQLClient();
            return sendGraphQLRequest(query, responseType, graphQLClient, retrievePath)
                    .block();
        } catch (Exception e) {
            throw new ApiClientException("API request failed", e);
        }
    }

    public static class ApiClientException extends RuntimeException {
        public ApiClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
