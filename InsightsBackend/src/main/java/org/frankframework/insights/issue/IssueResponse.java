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
    public String id;
    public int number;
    public String title;
    public GitHubPropertyState state;
    public OffsetDateTime closedAt;
    public String url;
    public String businessValue;
    public MilestoneResponse milestone;
    public IssueTypeResponse issueType;
    public IssuePriorityResponse issuePriority;
    public double points;
    public Set<LabelResponse> labels;
    public Set<IssueResponse> subIssues;
}
