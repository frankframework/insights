package org.frankframework.insights.github;

import java.util.Set;

public class GitHubPageExtractionResult<T> {
	final Set<T> entities;
	final boolean hasNextPage;
	final String endCursor;

	GitHubPageExtractionResult(Set<T> entities, boolean hasNextPage, String endCursor) {
		this.entities = entities;
		this.hasNextPage = hasNextPage;
		this.endCursor = endCursor;
	}
}
