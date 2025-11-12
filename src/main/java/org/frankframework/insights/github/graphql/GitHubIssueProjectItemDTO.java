package org.frankframework.insights.github.graphql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public record GitHubIssueProjectItemDTO(GitHubEdgesDTO<FieldValue> fieldValues) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FieldValue(String optionId, Double number, Field field) {
        public record Field(String name) {}
    }
}
