package org.frankframework.insights.github;

import java.util.List;

public record GitHubSingleSelectDTO(List<GitHubSingleSelectDTO.SingleSelectObject> nodes, GitHubPageInfo pageInfo) {
    public record SingleSelectObject(String name, List<GitHubSingleSelectProjectItemDTO> options) {}
}
