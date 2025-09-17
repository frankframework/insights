package org.frankframework.insights.pullrequest;

import java.time.OffsetDateTime;
import org.frankframework.insights.github.GitHubEdgesDTO;
import org.frankframework.insights.issue.IssueDTO;
import org.frankframework.insights.label.LabelDTO;
import org.frankframework.insights.milestone.MilestoneDTO;

public record PullRequestDTO(
        String id,
        int number,
        String title,
        String url,
        OffsetDateTime mergedAt,
        GitHubEdgesDTO<LabelDTO> labels,
        MilestoneDTO milestone,
        GitHubEdgesDTO<IssueDTO> closingIssuesReferences) {

    public boolean hasLabels() {
        return labels != null && labels.edges() != null && !labels.edges().isEmpty();
    }

    public boolean hasClosingIssuesReferences() {
        return closingIssuesReferences != null
                && closingIssuesReferences.edges() != null
                && !closingIssuesReferences.edges().isEmpty();
    }
}
