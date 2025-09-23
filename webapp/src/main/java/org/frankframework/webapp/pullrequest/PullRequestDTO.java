package org.frankframework.webapp.pullrequest;

import java.time.OffsetDateTime;
import org.frankframework.webapp.github.GitHubEdgesDTO;
import org.frankframework.webapp.issue.IssueDTO;
import org.frankframework.webapp.label.LabelDTO;
import org.frankframework.webapp.milestone.MilestoneDTO;

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
