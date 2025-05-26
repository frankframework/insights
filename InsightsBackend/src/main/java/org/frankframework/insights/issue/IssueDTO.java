package org.frankframework.insights.issue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import org.frankframework.insights.github.GitHubEdgesDTO;
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
        GitHubEdgesDTO<IssueDTO> subIssues) {

    public boolean hasLabels() {
        return labels != null && labels.getEdges() != null && !labels.getEdges().isEmpty();
    }

    public boolean hasSubIssues() {
        return subIssues != null
                && subIssues.getEdges() != null
                && !subIssues.getEdges().isEmpty();
    }
}
