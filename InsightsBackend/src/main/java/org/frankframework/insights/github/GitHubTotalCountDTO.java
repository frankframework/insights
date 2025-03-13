package org.frankframework.insights.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubTotalCountDTO(@JsonProperty("totalCount") int totalCount) {}
