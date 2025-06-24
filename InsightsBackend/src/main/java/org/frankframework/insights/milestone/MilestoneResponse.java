package org.frankframework.insights.milestone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.frankframework.insights.github.GitHubPropertyState;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MilestoneResponse(String id, int number, String title, String url, GitHubPropertyState state, OffsetDateTime dueOn, int openIssueCount, int closedIssueCount) {}
