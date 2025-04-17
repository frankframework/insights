package org.frankframework.insights.issue;

import java.time.OffsetDateTime;
import java.util.Set;
import org.frankframework.insights.github.GitHubPropertyState;
import org.frankframework.insights.milestone.MilestoneResponse;

public record IssueResponse(
        String id,
        int number,
        String title,
        GitHubPropertyState state,
        OffsetDateTime closedAt,
        String url,
        String businessValue,
        MilestoneResponse milestone,
        IssueResponse parentIssue,
        Set<IssueResponse> subIssues) {}
