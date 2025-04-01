package org.frankframework.insights.issue;

import org.frankframework.insights.github.GitHubEdgesDTO;
import org.frankframework.insights.github.GitHubPropertyState;
import org.frankframework.insights.label.LabelDTO;
import org.frankframework.insights.milestone.MilestoneDTO;

public record IssueDTO(
		String id,
		int number,
		String title,
		GitHubPropertyState state,
		String url,
		GitHubEdgesDTO<LabelDTO> labels,
		MilestoneDTO milestone
) {}
