package org.frankframework.insights.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GitHubRefsDTO(@JsonProperty("nodes") List<GitHubBranchNodeDTO> nodes) {

    public record GitHubBranchNodeDTO(
            @JsonProperty("name") String name, @JsonProperty("target") GitHubTargetDTO target, @JsonProperty("pullRequests") GitHubPullRequestDTO pullRequests) {}

    public record GitHubTargetDTO(@JsonProperty("history") GitHubTotalCountDTO history) {}

	public record GitHubPullRequestDTO(@JsonProperty("pullRequest") GitHubTotalCountDTO pullRequests) {}
}
