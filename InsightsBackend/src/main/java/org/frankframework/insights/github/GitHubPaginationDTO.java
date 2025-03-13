package org.frankframework.insights.github;

import java.util.List;

public class GitHubPaginationDTO<T> {
    public List<Edge<T>> edges;
    public PageInfo pageInfo;

    public static class Edge<T> {
        public T node;
    }

    public static class PageInfo {
        public boolean hasNextPage;
        public String endCursor;
    }
}
