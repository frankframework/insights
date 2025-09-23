package org.frankframework.webapp.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GitHubRefsDTO(@JsonProperty("nodes") List<GitHubBranchNodeDTO> nodes) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubBranchNodeDTO(
            @JsonProperty("name") String name, @JsonProperty("target") GitHubTargetDTO target) {}

    public record GitHubTargetDTO(@JsonProperty("history") GitHubTotalCountDTO history) {}
}
