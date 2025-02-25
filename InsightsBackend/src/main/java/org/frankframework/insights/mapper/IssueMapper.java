package org.frankframework.insights.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.frankframework.insights.models.Issue;
import org.frankframework.insights.models.Label;
import org.frankframework.insights.service.LabelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IssueMapper {
    private final ObjectMapper objectMapper;
    private final LabelService labelService;

    @Autowired
    public IssueMapper(ObjectMapper objectMapper, LabelService labelService) {
        this.objectMapper = objectMapper;
        this.labelService = labelService;
    }

    public Set<Issue> jsonToIssues(JsonNode jsonIssues) {
        return extractEdges(jsonIssues).map(this::jsonToIssue).collect(Collectors.toSet());
    }

    private Issue jsonToIssue(JsonNode jsonIssue) {
        try {
            JsonNode jsonIssueWithoutLabels = jsonIssue.deepCopy();
            ((ObjectNode) jsonIssueWithoutLabels).remove("labels");

            Issue issue = objectMapper.treeToValue(jsonIssueWithoutLabels, Issue.class);

            Set<String> labelNames = extractLabelNames(jsonIssue);

            if (!labelNames.isEmpty()) {
                return addLabelsToIssue(issue, labelNames);
            }

            return issue;
        } catch (Exception e) {
            throw new RuntimeException("Error mapping JSON to Issue", e);
        }
    }

    private Set<String> extractLabelNames(JsonNode jsonIssue) {
        JsonNode labelsNode = jsonIssue.path("labels").path("nodes");

        return StreamSupport.stream(labelsNode.spliterator(), false)
                .map(labelNode -> labelNode.path("name").asText())
                .collect(Collectors.toSet());
    }

    private Issue addLabelsToIssue(Issue issue, Set<String> labelNames) {
        Set<Label> labels = labelService.getAllLabelsByName(labelNames);
        issue.setLabels(labels);
        return issue;
    }

    private Stream<JsonNode> extractEdges(JsonNode jsonIssues) {
        return StreamSupport.stream(
                        jsonIssues
                                .path("data")
                                .path("repository")
                                .path("issues")
                                .path("edges")
                                .spliterator(),
                        false)
                .map(edge -> edge.path("node"));
    }
}
