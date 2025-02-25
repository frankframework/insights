package org.frankframework.insights.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GitHubClient extends ApiClient {

    private final String issueQuery;

    public GitHubClient(
            @Value("${github.api.url}") String gitHubBaseUrl,
            @Value("${github.api.secret}") String secretGitHub,
            @Value("${graphql.query.issues}") String issueQuery,
            ObjectMapper objectMapper) {
        super(gitHubBaseUrl, secretGitHub, objectMapper);
        this.issueQuery = issueQuery;
    }

    public JsonNode getLabels() {
        return request("/repos/frankframework/frankframework/labels");
    }

    public JsonNode getIssues() {
        JsonNode allIssuesResponse = null;
        String cursor = null;

        while (true) {
            JsonNode response = fetchEntityPage(issueQuery, cursor);
            if (allIssuesResponse == null) {
                allIssuesResponse = response;
            } else {
                mergeEntityObjects("issues", allIssuesResponse, response);
            }

            JsonNode issues =
                    response.path("data").path("repository").path("issues").path("edges");

            if (issues.size() < 100) {
                break;
            }

            cursor = getNextCursor("issues", response);
        }

        return allIssuesResponse;
    }

    private JsonNode fetchEntityPage(String query, String afterCursor) {
        String paginatedQuery = buildPaginatedQuery(query, afterCursor);
        return execute("/graphql", paginatedQuery);
    }

    private String buildPaginatedQuery(String baseQuery, String afterCursor) {
        if (afterCursor != null && !afterCursor.isEmpty()) {
            return baseQuery.replace("first: 100", "first: 100, after: \"" + afterCursor + "\"");
        }
        return baseQuery;
    }

    private void mergeEntityObjects(String entity, JsonNode existingResponse, JsonNode newResponse) {
        ArrayNode existingEdges = (ArrayNode)
                existingResponse.path("data").path("repository").path(entity).path("edges");
        ArrayNode newEdges = (ArrayNode)
                newResponse.path("data").path("repository").path(entity).path("edges");

        if (existingEdges != null && newEdges != null) {
            existingEdges.addAll(newEdges);
        }

        ObjectNode existingPageInfo = (ObjectNode)
                existingResponse.path("data").path("repository").path(entity).path("pageInfo");
        ObjectNode newPageInfo = (ObjectNode)
                newResponse.path("data").path("repository").path(entity).path("pageInfo");

        if (existingPageInfo != null && newPageInfo != null) {
            existingPageInfo.put("endCursor", newPageInfo.path("endCursor").asText());
            existingPageInfo.put("hasNextPage", newPageInfo.path("hasNextPage").asBoolean());
        }
    }

    private String getNextCursor(String entity, JsonNode response) {
        JsonNode pageInfo =
                response.path("data").path("repository").path(entity).path("pageInfo");
        return pageInfo.path("endCursor").asText();
    }
}
