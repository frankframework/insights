package org.frankframework.insights.github;

import java.util.List;

public record GitHubEdgesDTO<T>(List<GitHubNodeDTO<T>> edges) {}
