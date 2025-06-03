package org.frankframework.insights.github;

import java.util.List;

public class GitHubPaginationDTO<T> {
    public List<Edge<T>> edges;
    public GitHubPageInfo pageInfo;

    public static class Edge<T> {
        public T node;
    }
}
