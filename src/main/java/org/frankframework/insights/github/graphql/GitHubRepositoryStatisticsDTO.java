package org.frankframework.insights.github.graphql;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.regex.Pattern;

public record GitHubRepositoryStatisticsDTO(
        @JsonProperty("labels") GitHubTotalCountDTO labels,
        @JsonProperty("issueTypes") GitHubTotalCountDTO issueTypes,
        @JsonProperty("refs") GitHubRefsDTO branches) {

    public int getGitHubLabelCount() {
        return labels.totalCount();
    }

    public int getGitHubIssueTypeCount() {
        return issueTypes.totalCount();
    }

    public int getGitHubBranchCount(List<String> branchProtectionRegexes) {
        return (int) branches.nodes().stream()
                .filter(branch -> branchProtectionRegexes.stream()
                        .anyMatch(regex ->
                                Pattern.compile(regex).matcher(branch.name()).find()))
                .count();
    }
}
