package org.frankframework.insights.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RepositoryStatisticsDTO(
        @JsonProperty("labels") TotalCountDTO labels, @JsonProperty("milestones") TotalCountDTO milestones) {
    public int labelCount() {
        return labels != null ? labels.totalCount() : 0;
    }

    public int milestoneCount() {
        return milestones != null ? milestones.totalCount() : 0;
    }
}
