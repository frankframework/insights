package org.frankframework.insights.github.graphql;

import java.util.List;
import org.frankframework.insights.common.client.graphql.GraphQLPageInfoDTO;

public record GitHubSingleSelectDTO(List<SingleSelectObject> nodes, GraphQLPageInfoDTO pageInfo) {
	public record SingleSelectObject(String name, List<GitHubSingleSelectProjectItemDTO> options) {}
}
