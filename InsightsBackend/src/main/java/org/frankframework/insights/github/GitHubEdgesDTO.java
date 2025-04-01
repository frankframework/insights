package org.frankframework.insights.github;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class GitHubEdgesDTO<T> {
	@JsonProperty("edges")
	private List<GitHubNodeDTO<T>> edges;

	public List<GitHubNodeDTO<T>> getEdges() {
		return edges;
	}

	public void setEdges(List<GitHubNodeDTO<T>> edges) {
		this.edges = edges;
	}
}
