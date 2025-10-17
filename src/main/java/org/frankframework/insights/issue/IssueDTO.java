package org.frankframework.insights.issue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import org.frankframework.insights.github.GitHubEdgesDTO;
import org.frankframework.insights.github.GitHubIssueProjectItemDTO;
import org.frankframework.insights.github.GitHubNodeDTO;
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
        GitHubEdgesDTO<GitHubIssueProjectItemDTO> projectItems) {

    private static final String PRIORITY_FIELD_NAME = "Priority";
    private static final String STATE_FIELD_NAME = "Status";
    private static final String POINTS_FIELD_NAME = "Points";

    public boolean hasLabels() {
        return labels != null && labels.edges() != null && !labels.edges().isEmpty();
    }

    public boolean hasMilestone() {
        return milestone != null && milestone.id() != null;
    }

    public boolean hasIssueType() {
        return issueType != null && issueType.id() != null;
    }

    public boolean hasSubIssues() {
        return subIssues != null
                && subIssues.edges() != null
                && !subIssues.edges().isEmpty();
    }

    public Optional<String> findPriorityOptionId() {
        return findProjectField(IssueDTO.PRIORITY_FIELD_NAME, GitHubIssueProjectItemDTO.FieldValue::optionId);
    }

    public Optional<String> findStatusOptionId() {
        return findProjectField(IssueDTO.STATE_FIELD_NAME, GitHubIssueProjectItemDTO.FieldValue::optionId);
    }

    public Optional<Double> findPoints() {
        return findProjectField(IssueDTO.POINTS_FIELD_NAME, GitHubIssueProjectItemDTO.FieldValue::number);
    }

    private <T> Optional<T> findProjectField(
            String fieldName, java.util.function.Function<GitHubIssueProjectItemDTO.FieldValue, T> extractor) {
        if (projectItems.edges().isEmpty()) {
            return Optional.empty();
        }

        return projectItems.edges().stream()
                .map(GitHubNodeDTO::node)
                .map(GitHubIssueProjectItemDTO::fieldValues)
                .flatMap(fv -> {
                    if (fv.edges() == null || fv.edges().isEmpty()) return java.util.stream.Stream.empty();
                    return fv.edges().stream();
                })
                .map(GitHubNodeDTO::node)
                .filter(Objects::nonNull)
                .filter(fv -> fv.field() != null
                        && fieldName.equalsIgnoreCase(fv.field().name()))
                .map(extractor)
                .filter(Objects::nonNull)
                .findFirst();
    }
}
