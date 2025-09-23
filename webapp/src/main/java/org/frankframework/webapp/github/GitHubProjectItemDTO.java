package org.frankframework.webapp.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public record GitHubProjectItemDTO(GitHubEdgesDTO<FieldValue> fieldValues) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FieldValue(String optionId, Double number, Field field) {
        public record Field(String name) {}
    }
}
