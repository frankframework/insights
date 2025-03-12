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

    MAPPING_ERROR("MAPPING_ERROR", HttpStatus.BAD_REQUEST),

    LABEL_INJECTION_ERROR("LABEL_INJECTION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR),
    MILESTONE_INJECTION_ERROR("MILESTONE_INJECTION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR),
    BRANCH_INJECTION_ERROR("BRANCH_INJECTION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR),
    COMMIT_INJECTION_ERROR("COMMIT_INJECTION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR),
    RELEASE_INJECTION_ERROR("RELEASE_INJECTION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR),

    LABEL_DATABASE_ERROR("LABEL_DATABASE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR),
    MILESTONE_DATABASE_ERROR("MILESTONE_DATABASE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR),
    BRANCH_DATABASE_ERROR("BRANCH_DATABASE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR),
    COMMIT_DATABASE_ERROR("COMMIT_DATABASE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR),
    RELEASE_DATABASE_ERROR("RELEASE_DATABASE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final HttpStatus status;
}
