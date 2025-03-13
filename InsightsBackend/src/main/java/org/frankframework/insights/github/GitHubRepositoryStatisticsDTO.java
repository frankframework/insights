package org.frankframework.insights.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubRepositoryStatisticsDTO(
		@JsonProperty("labels") GitHubTotalCountDTO labels, @JsonProperty("milestones") GitHubTotalCountDTO milestones) {
    public int labelCount() {
        return labels != null ? labels.totalCount() : 0;
    }

    public int milestoneCount() {
        return milestones != null ? milestones.totalCount() : 0;
    }
}
