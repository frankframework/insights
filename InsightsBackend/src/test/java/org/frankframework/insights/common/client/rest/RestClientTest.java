package org.frankframework.insights.common.client.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class RestClientTest {

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private WebClient webClientMock;

	private TestRestClient testClient;

	private record MockDto(int id, String name) {}

	private static class TestRestClient extends RestClient {
		private final WebClient mockClient;

		TestRestClient(String baseUrl, Consumer<WebClient.Builder> configurer, WebClient mockClient) {
			super(baseUrl, configurer);
			this.mockClient = mockClient;
		}

		@Override
		protected WebClient getRestClient() {
			return mockClient;
		}
	}

	@BeforeEach
	public void setUp() {
		testClient = new TestRestClient("https://api.example.com", _ -> {}, webClientMock);
	}

	@Test
	public void get_SuccessWithString_ReturnsDeserializedString() throws RestClientException {
		String path = "/test";
		String expectedResponse = "Success";
		ParameterizedTypeReference<String> responseType = new ParameterizedTypeReference<>() {};

		when(webClientMock.get().uri(path).retrieve().bodyToMono(responseType))
				.thenReturn(Mono.just(expectedResponse));

		String actualResponse = testClient.get(path, responseType);

		assertThat(actualResponse).isEqualTo(expectedResponse);
	}

	@Test
	public void get_SuccessWithComplexObject_ReturnsDeserializedDto() throws RestClientException {
		String path = "/dto/1";
		MockDto expectedResponse = new MockDto(1, "Test DTO");
		ParameterizedTypeReference<MockDto> responseType = new ParameterizedTypeReference<>() {};

		when(webClientMock.get().uri(path).retrieve().bodyToMono(responseType))
				.thenReturn(Mono.just(expectedResponse));

		MockDto actualResponse = testClient.get(path, responseType);

		assertThat(actualResponse).isEqualTo(expectedResponse);
	}

	@Test
	public void get_ApiReturnsEmptyBody_ReturnsNull() throws RestClientException {
		String path = "/empty";
		ParameterizedTypeReference<String> responseType = new ParameterizedTypeReference<>() {};

		when(webClientMock.get().uri(path).retrieve().bodyToMono(responseType))
				.thenReturn(Mono.empty());

		String actualResponse = testClient.get(path, responseType);

		assertThat(actualResponse).isNull();
	}

	@Test
	public void get_ServerError500_ThrowsRestClientException() {
		String path = "/fail";
		ParameterizedTypeReference<String> responseType = new ParameterizedTypeReference<>() {};
		WebClientResponseException apiException = new WebClientResponseException(500, "Internal Server Error", null, null, null);

		when(webClientMock.get().uri(path).retrieve().bodyToMono(responseType))
				.thenReturn(Mono.error(apiException));

		assertThatThrownBy(() -> testClient.get(path, responseType))
				.isInstanceOf(RestClientException.class)
				.hasMessage("Failed GET request to path: /fail")
				.hasCause(apiException);
	}

	@Test
	public void get_NotFound404Error_ThrowsRestClientException() {
		String path = "/notfound";
		ParameterizedTypeReference<String> responseType = new ParameterizedTypeReference<>() {};
		WebClientResponseException apiException = WebClientResponseException.create(404, "Not Found", null, null, null, null);

		when(webClientMock.get().uri(path).retrieve().bodyToMono(responseType))
				.thenReturn(Mono.error(apiException));

		assertThatThrownBy(() -> testClient.get(path, responseType))
				.isInstanceOf(RestClientException.class)
				.hasMessage("Failed GET request to path: /notfound")
				.hasCause(apiException);
	}

	@Test
	public void get_Unauthorized401Error_ThrowsRestClientException() {
		String path = "/unauthorized";
		ParameterizedTypeReference<String> responseType = new ParameterizedTypeReference<>() {};
		WebClientResponseException apiException = WebClientResponseException.create(401, "Unauthorized", null, null, null, null);

		when(webClientMock.get().uri(path).retrieve().bodyToMono(responseType))
				.thenReturn(Mono.error(apiException));

		assertThatThrownBy(() -> testClient.get(path, responseType))
				.isInstanceOf(RestClientException.class)
				.hasMessage("Failed GET request to path: /unauthorized")
				.hasCause(apiException);
	}
}
