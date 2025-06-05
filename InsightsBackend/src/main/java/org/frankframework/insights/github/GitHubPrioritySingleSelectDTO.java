package org.frankframework.insights.github;

import java.util.List;
import org.frankframework.insights.issuePriority.IssuePriorityDTO;

public class GitHubPrioritySingleSelectDTO<T> {
    public List<GitHubPrioritySingleSelectDTO.SingleSelectObject<T>> nodes;
    public GitHubPageInfo pageInfo;

    public static class SingleSelectObject<T> {
        public String name;
        public List<IssuePriorityDTO> options;
    }
}
