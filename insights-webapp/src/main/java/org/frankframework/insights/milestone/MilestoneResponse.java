package org.frankframework.insights.milestone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import org.frankframework.insights.github.GitHubPropertyState;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MilestoneResponse(
        String id,
        int number,
        String title,
        String url,
        GitHubPropertyState state,
        OffsetDateTime dueOn,
        int openIssueCount,
        int closedIssueCount) {}
