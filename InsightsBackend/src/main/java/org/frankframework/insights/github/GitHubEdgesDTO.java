package org.frankframework.insights.github;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GitHubEdgesDTO<T> {
    private List<GitHubNodeDTO<T>> edges;
}
