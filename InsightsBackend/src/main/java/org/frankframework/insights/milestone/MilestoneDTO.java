package org.frankframework.insights.milestone;

import org.frankframework.insights.github.GitHubPropertyState;

public record MilestoneDTO(String id, int number, String title, GitHubPropertyState state) {}
