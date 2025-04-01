package org.frankframework.insights.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GitHubNodeDTO<T> {
	@JsonProperty("node")
	private T node;

	public T getNode() {
		return node;
	}

	public void setNode(T node) {
		this.node = node;
	}
}
