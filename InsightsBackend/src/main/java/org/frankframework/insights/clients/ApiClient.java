package org.frankframework.insights.clients;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

public abstract class ApiClient {
	protected String baseUrl;
	protected final String secret;

	private final RestTemplate restTemplate = new RestTemplate();

	public ApiClient(String baseUrl, String secret) {
		this.baseUrl = baseUrl;
		this.secret = secret;
	}

	public JsonNode execute(String path, String query) {
		try {
			HttpHeaders headers = makeHeaders();
			HttpEntity<String> entity = new HttpEntity<>(query, headers);


			ResponseEntity<JsonNode> response = restTemplate.exchange(
					buildUri(path), HttpMethod.POST, entity, JsonNode.class);

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

			ResponseEntity<JsonNode> response = restTemplate.exchange(
					buildUri(path), HttpMethod.GET, entity, JsonNode.class);

			return response.getBody();
		} catch (HttpClientErrorException | HttpServerErrorException e) {
			throw new ApiClientException("API request failed with status: " + e.getStatusCode(), e);
		} catch (Exception e) {
			throw new ApiClientException("Unexpected error occurred during API request", e);
		}
	}

	private URI buildUri(String path) {
		return UriComponentsBuilder.fromUriString(baseUrl)
				.path(path)
				.build()
				.toUri();
	}

	private HttpHeaders makeHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(secret);
		return headers;
	}

	// todo change exception to exception handler folder?
	public static class ApiClientException extends RuntimeException {
		public ApiClientException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
