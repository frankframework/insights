package org.frankframework.insights.github;

import org.frankframework.insights.issuePriority.IssuePriorityDTO;

import java.util.List;

public class GitHubSingleSelectDTO<T> {
	public List<GitHubSingleSelectDTO.SingleSelectObject<T>> nodes;
	public GitHubPageInfo pageInfo;

	public static class SingleSelectObject<T> {
		public String name;
		public List<IssuePriorityDTO> options;
	}
}
