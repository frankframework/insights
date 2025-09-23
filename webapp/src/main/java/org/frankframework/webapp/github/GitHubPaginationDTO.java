package org.frankframework.webapp.github;

import java.util.List;

public record GitHubPaginationDTO<T>(List<GitHubNodeDTO<T>> edges, GitHubPageInfo pageInfo) {}
