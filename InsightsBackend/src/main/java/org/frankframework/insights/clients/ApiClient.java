package org.frankframework.insights.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public abstract class ApiClient {
    protected String baseUrl;
    protected final String secret;

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper;

    public ApiClient(String baseUrl, String secret, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.secret = secret;
        this.objectMapper = objectMapper;
    }

    public JsonNode execute(String path, String query) {
        try {
            HttpHeaders headers = makeHeaders();
            HttpEntity<String> entity = new HttpEntity<>(buildQuery(query), headers);

            ResponseEntity<JsonNode> response =
                    restTemplate.exchange(buildUri(path), HttpMethod.POST, entity, JsonNode.class);

            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new ApiClientException("API request failed with status: " + e.getStatusCode(), e);
        } catch (Exception e) {
            throw new ApiClientException("Unexpected error occurred during API request", e);
        }
    }

    public JsonNode request(String path) {
        try {
            HttpHeaders headers = makeHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<JsonNode> response =
                    restTemplate.exchange(buildUri(path), HttpMethod.GET, entity, JsonNode.class);

            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new ApiClientException("API request failed with status: " + e.getStatusCode(), e);
        } catch (Exception e) {
            throw new ApiClientException("Unexpected error occurred during API request", e);
        }
    }

    private HttpHeaders makeHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(secret);
        return headers;
    }

    private URI buildUri(String path) {
        return UriComponentsBuilder.fromUriString(baseUrl).path(path).build().toUri();
    }

    private String buildQuery(String query) throws JsonProcessingException {
        return objectMapper.writeValueAsString(Map.of("query", query));
    }

    // todo add global exception handler to catch exceptions like above
    public static class ApiClientException extends RuntimeException {
        public ApiClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
