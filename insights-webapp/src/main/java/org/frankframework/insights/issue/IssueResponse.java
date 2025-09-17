package org.frankframework.insights.issue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.frankframework.insights.github.GitHubPropertyState;
import org.frankframework.insights.issuePriority.IssuePriorityResponse;
import org.frankframework.insights.issuetype.IssueTypeResponse;
import org.frankframework.insights.label.LabelResponse;
import org.frankframework.insights.milestone.MilestoneResponse;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class IssueResponse {
    private String id;
    private int number;
    private String title;
    private GitHubPropertyState state;
    private OffsetDateTime closedAt;
    private String url;
    private String businessValue;
    private MilestoneResponse milestone;
    private IssueTypeResponse issueType;
    private IssuePriorityResponse issuePriority;
    private double points;
    private Set<LabelResponse> labels;
    private Set<IssueResponse> subIssues;
}
