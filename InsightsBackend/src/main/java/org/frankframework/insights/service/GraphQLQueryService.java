package org.frankframework.insights.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GraphQLQueryService {
    private final List<JsonNode> queries;

    public GraphQLQueryService(List<JsonNode> queries) {
        this.queries = queries;
    }

    public JsonNode customizeQuery(int queryIndex, String afterCursor, String tagName, String commitSHA) {
        ObjectNode queryNode = queries.get(queryIndex).deepCopy();
        JsonNode variablesNode = queryNode.get("variables");

        if (variablesNode instanceof ObjectNode variables) {
            updateVariableIfPresent(variables, "after", afterCursor);
            updateVariableIfPresent(variables, "tagName", tagName);
            updateVariableIfPresent(variables, "commitSHA", commitSHA);
        }

        return queryNode;
    }

    private void updateVariableIfPresent(ObjectNode variables, String key, String value) {
        if (value != null && variables.has(key)) {
            variables.put(key, value);
        }
    }
}
