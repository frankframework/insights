package org.frankframework.insights.pullrequest;

import org.frankframework.insights.github.GitHubEdgesDTO;
import org.frankframework.insights.github.GitHubPropertyState;
import org.frankframework.insights.label.LabelDTO;
import org.frankframework.insights.milestone.MilestoneDTO;

import java.time.OffsetDateTime;

public record PullRequestDTO (
		String id,
		int number,
		String title,
		GitHubPropertyState state,
		String url,
		OffsetDateTime mergedAt,
		GitHubEdgesDTO<LabelDTO> labels,
		MilestoneDTO milestone
	) {
}
