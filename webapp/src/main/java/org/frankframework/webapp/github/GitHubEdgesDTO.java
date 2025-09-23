package org.frankframework.webapp.github;

import java.util.List;

public record GitHubEdgesDTO<T>(List<GitHubNodeDTO<T>> edges) {}
