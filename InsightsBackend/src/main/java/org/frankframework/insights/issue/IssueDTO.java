package org.frankframework.insights.issue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import org.frankframework.insights.github.GitHubEdgesDTO;
import org.frankframework.insights.github.GitHubNodeDTO;
import org.frankframework.insights.github.GitHubProjectItemDTO;
import org.frankframework.insights.github.GitHubPropertyState;
import org.frankframework.insights.issuetype.IssueTypeDTO;
import org.frankframework.insights.label.LabelDTO;
import org.frankframework.insights.milestone.MilestoneDTO;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IssueDTO(
        String id,
        int number,
        String title,
        GitHubPropertyState state,
        OffsetDateTime closedAt,
        String url,
        GitHubEdgesDTO<LabelDTO> labels,
        MilestoneDTO milestone,
        IssueTypeDTO issueType,
        GitHubEdgesDTO<IssueDTO> subIssues,
        GitHubEdgesDTO<GitHubProjectItemDTO> projectItems) {

    public static final String PRIORITY_FIELD_NAME = "Priority";
    public static final String POINTS_FIELD_NAME = "Points";

    public boolean hasLabels() {
        return labels != null && labels.getEdges() != null && !labels.getEdges().isEmpty();
    }

    public boolean hasMilestone() {
        return milestone != null && milestone.id() != null;
    }

    public boolean hasIssueType() {
        return issueType != null && issueType.id != null;
    }

    public boolean hasSubIssues() {
        return subIssues != null
                && subIssues.getEdges() != null
                && !subIssues.getEdges().isEmpty();
    }

    public Optional<String> findPriorityOptionId() {
        return findProjectField(IssueDTO.PRIORITY_FIELD_NAME, fv -> fv.optionId);
    }

    public Optional<Double> findPoints() {
        return findProjectField(IssueDTO.POINTS_FIELD_NAME, fv -> fv.number);
    }

    private <T> Optional<T> findProjectField(
            String fieldName, java.util.function.Function<GitHubProjectItemDTO.FieldValue, T> extractor) {
        if (projectItems.getEdges().isEmpty()) {
            return Optional.empty();
        }

        return projectItems.getEdges().stream()
                .map(GitHubNodeDTO::getNode)
                .map(item -> item.fieldValues)
                .flatMap(fv -> {
                    if (fv.getEdges() == null || fv.getEdges().isEmpty()) return java.util.stream.Stream.empty();
                    return fv.getEdges().stream();
                })
                .map(GitHubNodeDTO::getNode)
                .filter(Objects::nonNull)
                .filter(fv -> fv.field != null && fieldName.equalsIgnoreCase(fv.field.name))
                .map(extractor)
                .filter(Objects::nonNull)
                .findFirst();
    }
}
