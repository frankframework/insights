package org.frankframework.insights.github;

import java.util.List;
import org.frankframework.insights.common.client.graphql.GraphQLPageInfoDTO;
import org.frankframework.insights.issuePriority.IssuePriorityDTO;

public record GitHubPrioritySingleSelectDTO(
        List<GitHubPrioritySingleSelectDTO.SingleSelectObject> nodes, GraphQLPageInfoDTO pageInfo) {
    public record SingleSelectObject(String name, List<IssuePriorityDTO> options) {}
}
