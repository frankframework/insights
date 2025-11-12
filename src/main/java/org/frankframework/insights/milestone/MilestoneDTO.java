package org.frankframework.insights.milestone;

import java.time.OffsetDateTime;
import org.frankframework.insights.github.graphql.GitHubPropertyState;

public record MilestoneDTO(
        String id,
        int number,
        String title,
        String url,
        GitHubPropertyState state,
        OffsetDateTime dueOn,
        int openIssueCount,
        int closedIssueCount) {}
