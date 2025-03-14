package org.frankframework.insights.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubCommitNodeDTO(
        @JsonProperty("name") String name, @JsonProperty("target.history") GitHubTotalCountDTO commitCount) {}
