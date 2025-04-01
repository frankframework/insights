package org.frankframework.insights.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.regex.Pattern;

public record GitHubRepositoryStatisticsDTO(
        @JsonProperty("labels") GitHubTotalCountDTO labels,
        @JsonProperty("milestones") GitHubTotalCountDTO milestones,
        @JsonProperty("refs") GitHubRefsDTO branches,
        @JsonProperty("releases") GitHubTotalCountDTO releases) {
    public int getGitHubLabelCount() {
        return labels.totalCount();
    }

    public int getGitHubMilestoneCount() {
        return milestones.totalCount();
    }

    public int getGitHubBranchCount(List<String> branchProtectionRegexes) {
        return (int) branches.nodes().stream()
                .filter(branch -> branchProtectionRegexes.stream()
                        .anyMatch(regex ->
                                Pattern.compile(regex).matcher(branch.name()).find()))
                .count();
    }

    public int getGitHubCommitCount(List<String> branchProtectionRegexes) {
        return branches.nodes().stream()
                .filter(branch -> branchProtectionRegexes.stream()
                        .anyMatch(regex ->
                                Pattern.compile(regex).matcher(branch.name()).find()))
                .mapToInt(branch -> branch.target().history().totalCount())
                .sum();
    }

    public int getGitHubReleaseCount() {
        return releases.totalCount();
    }
}
