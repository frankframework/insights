package org.frankframework.insights.clients;

import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

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

    protected HttpGraphQlClient getGraphQlClient() {
        return graphQlClient;
    }
}
