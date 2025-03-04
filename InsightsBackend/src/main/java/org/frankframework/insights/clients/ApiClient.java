package org.frankframework.insights.clients;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

public abstract class ApiClient {
    protected String baseUrl;
    protected final String secret;
    private final RestTemplate restTemplate = new RestTemplate();

    public ApiClient(String baseUrl, String secret) {
        this.baseUrl = baseUrl;
        this.secret = secret;
    }

    public <T> T request(JsonNode query, ParameterizedTypeReference<T> responseType) {
        try {
            ResponseEntity<T> response =
                    restTemplate.exchange(baseUrl, HttpMethod.POST, buildHttpEntity(query), responseType);
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new ApiClientException("API request failed with status: " + e.getStatusCode(), e);
        } catch (Exception e) {
            throw new ApiClientException("Unexpected error occurred during API request", e);
        }
    }

    private HttpEntity<JsonNode> buildHttpEntity(JsonNode query) {
        HttpHeaders headers = buildHeaders();
        if (query == null) {
            return new HttpEntity<>(headers);
        }
        return new HttpEntity<>(query, headers);
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(secret);
        return headers;
    }

    // todo add global exception handler to catch exceptions like above
    public static class ApiClientException extends RuntimeException {
        public ApiClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
