package org.frankframework.insights.milestone;

import org.frankframework.insights.github.GitHubPropertyState;

public record MilestoneResponse(String id, int number, GitHubPropertyState state, String title) {}
