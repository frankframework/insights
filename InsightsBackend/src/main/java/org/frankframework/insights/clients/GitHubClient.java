package org.frankframework.insights.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.frankframework.insights.configuration.GitHubProperties;
import org.frankframework.insights.dto.GraphQLDTO;
import org.frankframework.insights.dto.LabelDTO;
import org.frankframework.insights.dto.MilestoneDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class GitHubClient extends GraphQLClient {

    private final HttpGraphQlClient graphQlClient;
    private final ObjectMapper objectMapper;

    public GitHubClient(GitHubProperties gitHubProperties, ObjectMapper objectMapper) {
        super(gitHubProperties.getUrl(), gitHubProperties.getSecret());
        this.graphQlClient = getGraphQLClient();
        this.objectMapper = objectMapper;
    }

    public Set<LabelDTO> getLabels() {
        return getEntities("labels", "repository.labels", LabelDTO.class, Map.of());
    }

    public Set<MilestoneDTO> getMilestones() {
        return getEntities("milestones", "repository.milestones", MilestoneDTO.class, Map.of());
    }

    public <T> Set<T> getEntities(
            String documentName, String retrievePath, Class<T> entityType, Map<String, Object> extraVariables) {
        Set<T> allEntities = new HashSet<>();
        String cursor = null;
        boolean hasNextPage = true;

        while (hasNextPage) {
            GraphQLDTO<T> response = fetchEntityPage(documentName, retrievePath, cursor, entityType, extraVariables);

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

    private <T> GraphQLDTO<T> fetchEntityPage(
            String documentName,
            String retrievePath,
            String afterCursor,
            Class<T> entityType,
            Map<String, Object> extraVariables) {
        HttpGraphQlClient.RequestSpec request =
                graphQlClient.documentName(documentName).variable("after", afterCursor);

        if (extraVariables != null) {
            extraVariables.forEach(request::variable);
        }

        return request.retrieve(retrievePath)
                .toEntity(new ParameterizedTypeReference<GraphQLDTO<T>>() {})
                .block();
    }

    @Override
    protected <T> Mono<T> sendGraphQLRequest(
            JsonNode query,
            ParameterizedTypeReference<T> responseType,
            HttpGraphQlClient graphQLClient,
            String retrievePath) {
        return graphQLClient
                .mutate()
                .build()
                .document(query.toString())
                .retrieve(retrievePath)
                .toEntity(responseType);
    }
}
