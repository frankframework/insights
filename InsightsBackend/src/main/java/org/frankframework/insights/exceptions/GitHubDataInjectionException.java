package org.frankframework.insights.exceptions;

import org.frankframework.insights.enums.ErrorCode;

public class GitHubDataInjectionException extends ApiException {

	public GitHubDataInjectionException() {
		super("Failed to inject GitHub data in the database.", ErrorCode.GITHUB_DATA_INJECTION_ERROR);
	}
}
