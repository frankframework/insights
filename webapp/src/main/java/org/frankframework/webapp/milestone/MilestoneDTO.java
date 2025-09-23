package org.frankframework.webapp.milestone;

import java.time.OffsetDateTime;
import org.frankframework.webapp.github.GitHubPropertyState;

public record MilestoneDTO(
        String id,
        int number,
        String title,
        String url,
        GitHubPropertyState state,
        OffsetDateTime dueOn,
        int openIssueCount,
        int closedIssueCount) {}
