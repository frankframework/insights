package org.frankframework.insights.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public record GitHubRepositoryStatisticsDTO(
        @JsonProperty("labels") GitHubTotalCountDTO labels,
        @JsonProperty("milestones") GitHubTotalCountDTO milestones,
        @JsonProperty("refs") GitHubTotalCountDTO branches,
        @JsonProperty("refs.nodes") Set<GitHubCommitNodeDTO> branchesNodes,
        @JsonProperty("releases") GitHubTotalCountDTO releases) {
    public int getGitHubLabelCount() {
        return labels.totalCount();
    }

    public int getGitHubMilestoneCount() {
        return milestones.totalCount();
    }

    public int getGitHubBranchCount() {
        return branches.totalCount();
    }

    public int getGitHubCommitCount(List<String> branchProtectionRegexes) {
        return branchesNodes.stream()
                .filter(branch -> branchProtectionRegexes.stream()
                        .anyMatch(regex ->
                                Pattern.compile(regex).matcher(branch.name()).find()))
                .mapToInt(branch -> branch.commitCount().totalCount())
                .sum();
    }

    public int getGitHubReleaseCount() {
        return releases.totalCount();
    }
}
