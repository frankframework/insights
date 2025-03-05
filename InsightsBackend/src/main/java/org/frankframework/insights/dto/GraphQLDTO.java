package org.frankframework.insights.dto;

import java.util.List;

public class GraphQLDTO<T> {
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
