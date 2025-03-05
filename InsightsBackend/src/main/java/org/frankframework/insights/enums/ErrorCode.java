package org.frankframework.insights.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    BAD_REQUEST_ERROR("BAD_REQUEST_ERROR", HttpStatus.BAD_REQUEST),

    GRAPHQL_CLIENT_ERROR("GRAPHQL_CLIENT_ERROR", HttpStatus.SERVICE_UNAVAILABLE),

    GITHUB_CLIENT_ERROR("GITHUB_CLIENT_ERROR", HttpStatus.INTERNAL_SERVER_ERROR),

    LABEL_MAPPING_ERROR("LABEL_MAPPING_ERROR", HttpStatus.BAD_REQUEST),

    MILESTONE_MAPPING_ERROR("MILESTONE_MAPPING_ERROR", HttpStatus.BAD_REQUEST),

    LABEL_DATABASE_ERROR("LABEL_DATABASE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR),

    MILESTONE_DATABASE_ERROR("MILESTONE_DATABASE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final HttpStatus status;
}
