package org.frankframework.insights.github;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GitHubRefsDTO(
		@JsonProperty("nodes") List<GitHubCommitNodeDTO> nodes) {

	public record GitHubCommitNodeDTO(
			@JsonProperty("name") String name,
			@JsonProperty("target") GitHubTargetDTO target) {
	}

	public record GitHubTargetDTO(
			@JsonProperty("history") GitHubTotalCountDTO history) {}
}
