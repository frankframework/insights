package org.frankframework.insights.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record GitHubRepositoryStatisticsDTO(
        @JsonProperty("labels") GitHubTotalCountDTO labels,
        @JsonProperty("milestones") GitHubTotalCountDTO milestones,
        @JsonProperty("refs") GitHubRefsDTO branches,
        @JsonProperty("releases") GitHubTotalCountDTO releases,
        @JsonProperty("issues") GitHubTotalCountDTO issues) {

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

    public Map<String, Integer> getGitHubCommitsCount(List<String> branchProtectionRegexes) {
        return branches.nodes().stream()
                .filter(branch -> branchProtectionRegexes.stream()
                        .anyMatch(regex ->
                                Pattern.compile(regex).matcher(branch.name()).find()))
                .collect(Collectors.toMap(
                        GitHubRefsDTO.GitHubBranchNodeDTO::name,
                        branch -> branch.target().history().totalCount()));
    }

    public int getGitHubReleaseCount() {
        return releases.totalCount();
    }

    public int getGitHubIssueCount() {
        return issues.totalCount();
    }
}
