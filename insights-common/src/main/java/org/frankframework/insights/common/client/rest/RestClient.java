package org.frankframework.insights.common.client.rest;

import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.client.ApiClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
public abstract class RestClient extends ApiClient {
    /**
     * Flexible constructor for RestClient.
     * @param baseUrl the base URL of the external REST server (mandatory).
     * @param configurer a Consumer that receives the WebClient.Builder for further configuration, such as adding authentication headers. Can be null.
     */
    public RestClient(String baseUrl, Consumer<WebClient.Builder> configurer) {
        super(baseUrl, configurer);
    }

    /**
     * Executes a GET request to a specified path.
     * @param path The endpoint path to request.
     * @param responseType The class or type reference of the expected response.
     * @param <T> The type of the response body.
     * @return The deserialized response body.
     * @throws RestClientException if the request fails.
     */
    protected <T> T get(String path, ParameterizedTypeReference<T> responseType) throws RestClientException {
        try {
            return getRestClient()
                    .get()
                    .uri(path)
                    .retrieve()
                    .bodyToMono(responseType)
                    .block();
        } catch (Exception e) {
            throw new RestClientException(String.format("Failed GET request to path: %s", path), e);
        }
    }

    /**
     * Provides access to the configured WebClient instance.
     * @return the WebClient instance.
     */
    protected WebClient getRestClient() {
        return this.webClient;
    }
}
