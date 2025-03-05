package org.frankframework.insights.clients;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public abstract class GraphQLClient {
    private final HttpGraphQlClient graphQlClient;

    public GraphQLClient(String baseUrl, String secret) {
        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + secret)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();

        this.graphQlClient = HttpGraphQlClient.builder(webClient).build();
    }

    protected <T> Mono<T> sendGraphQLRequest(
            JsonNode query, ParameterizedTypeReference<T> responseType, String retrievePath) {
        return graphQlClient.document(query.toString()).retrieve(retrievePath).toEntity(responseType);
    }

    protected HttpGraphQlClient getGraphQlClient() {
        return graphQlClient;
    }
}
