package org.frankframework.insights.milestone;

import org.frankframework.insights.github.GitHubPropertyState;

import java.time.OffsetDateTime;

public record MilestoneDTO(String id, int number, String title, String url, GitHubPropertyState state, OffsetDateTime dueOn, int openIssueCount, int closedIssueCount) {}
