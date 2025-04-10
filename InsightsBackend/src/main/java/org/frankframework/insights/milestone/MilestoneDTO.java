package org.frankframework.insights.milestone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.frankframework.insights.github.GitHubPropertyState;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MilestoneDTO(String id, int number, GitHubPropertyState state, String title) {}
