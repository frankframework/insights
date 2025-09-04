package org.frankframework.insights.github;

import java.util.List;
import org.frankframework.insights.common.client.graphql.GraphQLPageInfoDTO;

public record GitHubNodesDTO<T>(List<T> nodes, GraphQLPageInfoDTO pageInfo) {}
