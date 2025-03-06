package org.frankframework.insights.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.frankframework.insights.configuration.GitHubProperties;
import org.frankframework.insights.dto.GraphQLDTO;
import org.frankframework.insights.dto.LabelDTO;
import org.frankframework.insights.dto.MilestoneDTO;
import org.frankframework.insights.enums.GraphQLConstants;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
public class GitHubClient extends GraphQLClient {
    private final ObjectMapper objectMapper;

    public GitHubClient(GitHubProperties gitHubProperties, ObjectMapper objectMapper) {
        super(gitHubProperties.getUrl(), gitHubProperties.getSecret());
        this.objectMapper = objectMapper;
    }

    public Set<LabelDTO> getLabels() {
        return getEntities(GraphQLConstants.LABELS, LabelDTO.class);
    }

    public Set<MilestoneDTO> getMilestones() {
        return getEntities(GraphQLConstants.MILESTONES, MilestoneDTO.class);
    }

    private <T> Set<T> getEntities(GraphQLConstants query, Class<T> entityType) {
        Set<T> allEntities = new HashSet<>();
        String cursor = null;
        boolean hasNextPage = true;

        while (hasNextPage) {
            GraphQLDTO<T> response = fetchEntityPage(query, cursor, entityType);

            if (response == null || response.edges == null) {
                break;
            }

            allEntities.addAll(response.edges.stream()
                    .map(edge -> objectMapper.convertValue(edge.node, entityType))
                    .collect(Collectors.toSet()));

            hasNextPage = response.pageInfo != null && response.pageInfo.hasNextPage;
            cursor = response.pageInfo != null ? response.pageInfo.endCursor : null;
        }

        return allEntities;
    }

    private <T> GraphQLDTO<T> fetchEntityPage(GraphQLConstants query, String afterCursor, Class<T> entityType) {
        return getGraphQlClient()
                .documentName(query.getDocumentName())
                .variable("after", afterCursor)
                .retrieve(query.getRetrievePath())
                .toEntity(new ParameterizedTypeReference<GraphQLDTO<T>>() {})
                .block();
    }
}
