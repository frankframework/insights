package org.frankframework.insights.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

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

	public <T> T request(String path, HttpMethod httpMethod, ParameterizedTypeReference<T> responseType, String query) {
		try {
			ResponseEntity<T> response =
					restTemplate.exchange(buildUri(path), httpMethod, buildHttpEntity(query), responseType);
			return response.getBody();
		} catch (HttpClientErrorException | HttpServerErrorException e) {
			throw new ApiClientException("API request failed with status: " + e.getStatusCode(), e);
		} catch (Exception e) {
			throw new ApiClientException("Unexpected error occurred during API request", e);
		}
	}

	private HttpEntity<String> buildHttpEntity(String query) throws JsonProcessingException {
		HttpHeaders headers = buildHeaders();
		if (query == null) {
			return new HttpEntity<>(headers);
		}
		return new HttpEntity<>(buildQuery(query), headers);
	}

	private HttpHeaders buildHeaders() {
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
