package org.frankframework.insights.clients;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GitHubClient extends ApiClient {

	public GitHubClient(
			@Value("${github.api.url}") String gitHubBaseUrl,
			@Value("${github.api.secret}") String secretGitHub
	) {
		super(gitHubBaseUrl, secretGitHub);
	}

	public JsonNode getLabels() {
		return request("/repos/frankframework/frankframework/labels");
	}
}
