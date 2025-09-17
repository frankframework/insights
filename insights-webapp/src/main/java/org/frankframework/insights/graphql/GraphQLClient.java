package org.frankframework.insights.graphql;

import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
public abstract class GraphQLClient {
    private final HttpGraphQlClient graphQlClient;

    /**
     * Constructor for GraphQLClient.
     * @param baseUrl the base URL of the external GraphQL server
     * @param secret the secret token for authentication
     */
    public GraphQLClient(String baseUrl, String secret) {
        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + secret)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();

        this.graphQlClient = HttpGraphQlClient.builder(webClient).build();
        log.info("GraphQLClient initialized successfully with base URL: {}", baseUrl);
    }

    protected HttpGraphQlClient getGraphQlClient() {
        return graphQlClient;
    }
}
