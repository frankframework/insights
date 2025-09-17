package org.frankframework.insights.github;

import java.util.List;
import org.frankframework.insights.issuePriority.IssuePriorityDTO;

public record GitHubPrioritySingleSelectDTO(
        List<GitHubPrioritySingleSelectDTO.SingleSelectObject> nodes, GitHubPageInfo pageInfo) {
    public record SingleSelectObject(String name, List<IssuePriorityDTO> options) {}
}
