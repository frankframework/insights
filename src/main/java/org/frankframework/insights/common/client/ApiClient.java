package org.frankframework.insights.common.client;

import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * The abstract base class for all external API clients.
 * It centralizes the creation and configuration of the Spring WebClient.
 */
@Slf4j
public abstract class ApiClient {

    protected final WebClient webClient;

    /**
     * Flexible constructor for any WebClient-based client.
     * @param baseUrl the base URL of the external server (mandatory).
     * @param configurer a Consumer that configures the WebClient.Builder, e.g., for auth headers.
     */
    public ApiClient(String baseUrl, Consumer<WebClient.Builder> configurer) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Base URL cannot be null or empty.");
        }

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        if (configurer != null) {
            configurer.accept(builder);
        }

        this.webClient = builder.build();
        log.info(
                "WebClient initialized successfully for {} with base URL: {}",
                this.getClass().getSimpleName(),
                baseUrl);
    }
}
