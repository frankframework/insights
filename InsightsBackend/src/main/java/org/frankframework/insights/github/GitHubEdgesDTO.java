package org.frankframework.insights.github;

import java.util.List;
import org.frankframework.insights.common.client.graphql.GraphQLNodeDTO;

public record GitHubEdgesDTO<T>(List<GraphQLNodeDTO<T>> edges) {}
