package org.frankframework.insights.common.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@ExtendWith(MockitoExtension.class)
public class ApiClientTest {
    private static class TestApiClient extends ApiClient {
        TestApiClient(String baseUrl, Consumer<WebClient.Builder> configurer) {
            super(baseUrl, configurer);
        }
    }

    @Test
    public void constructor_withValidBaseUrl_initializesClient() {
        String baseUrl = "https://api.example.com";

        TestApiClient client = new TestApiClient(baseUrl, null);

        assertThat(client.webClient).isNotNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    public void constructor_withInvalidBaseUrl_throwsIllegalArgumentException(String baseUrl) {
        assertThatThrownBy(() -> new TestApiClient(baseUrl, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Base URL cannot be null or empty.");
    }

    @Test
    public void constructor_withConfigurer_appliesConfiguration() {
        String baseUrl = "https://api.example.com";
        Consumer<WebClient.Builder> configurer = b -> b.defaultHeader(HttpHeaders.AUTHORIZATION, "test-token");

        WebClient.Builder realBuilder = WebClient.builder();

        configurer.accept(realBuilder);

        ApiClient client = new ApiClient(baseUrl, b -> b.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer token")) {};

        assertThat(client.webClient).isNotNull();
    }

    @Test
    public void constructor_withNullConfigurer_doesNotThrowException() {
        String baseUrl = "https://api.example.com";

        TestApiClient client = new TestApiClient(baseUrl, null);

        assertThat(client.webClient).isNotNull();
    }
}
