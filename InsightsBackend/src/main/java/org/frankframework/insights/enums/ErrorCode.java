package org.frankframework.insights.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    BAD_REQUEST_ERROR("BAD_REQUEST_ERROR", HttpStatus.BAD_REQUEST),
    REST_API_REQUEST_ERROR("REST_API_REQUEST_ERROR", HttpStatus.SERVICE_UNAVAILABLE),

    GRAPHQL_API_REQUEST_ERROR("GRAPHQL_API_REQUEST_ERROR", HttpStatus.SERVICE_UNAVAILABLE),

    UNEXPECTED_API_CLIENT_ERROR("UNEXPECTED_API_CLIENT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR),

	GITHUB_DATA_INJECTION_ERROR("GITHUB_DATA_INJECTION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);

	private final String code;
    private final HttpStatus status;
}
