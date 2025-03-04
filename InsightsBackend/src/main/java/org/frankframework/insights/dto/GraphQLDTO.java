package org.frankframework.insights.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphQLDTO<T> {
    public Data<T> data;

    public static class Data<T> {
        public Repository<T> repository;
    }

    public static class Repository<T> {
        private final Map<String, EntityConnection<T>> entities = new HashMap<>();

        @JsonAnySetter
        public void addEntity(String key, EntityConnection<T> value) {
            entities.put(key, value);
        }

        @JsonAnyGetter
        public Map<String, EntityConnection<T>> getEntities() {
            return entities;
        }
    }

    public static class EntityConnection<T> {
        public List<Edge<T>> edges;
        public PageInfo pageInfo;
    }

    public static class Edge<T> {
        public T node;
    }

    public static class PageInfo {
        public boolean hasNextPage;
        public String endCursor;
    }
}
