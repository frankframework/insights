package org.frankframework.insights.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.configuration.GitHubProperties;
import org.frankframework.insights.dto.GraphQLDTO;
import org.frankframework.insights.dto.LabelDTO;
import org.frankframework.insights.dto.MilestoneDTO;
import org.frankframework.insights.dto.RepositoryStatisticsDTO;
import org.frankframework.insights.enums.GraphQLConstants;
import org.frankframework.insights.exceptions.clients.GitHubClientException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GitHubClient extends GraphQLClient {
    private final ObjectMapper objectMapper;

    public GitHubClient(GitHubProperties gitHubProperties, ObjectMapper objectMapper) {
        super(gitHubProperties.getUrl(), gitHubProperties.getSecret());
        this.objectMapper = objectMapper;
    }

    public RepositoryStatisticsDTO getRepositoryStatistics() throws GitHubClientException {
        RepositoryStatisticsDTO statistics =
                fetchSingleEntity(GraphQLConstants.REPOSITORY_STATISTICS, RepositoryStatisticsDTO.class);
        log.info("Fetched repository statistics from GitHub");
        return statistics;
    }

    public Set<LabelDTO> getLabels() throws GitHubClientException {
        Set<LabelDTO> labels = getEntities(GraphQLConstants.LABELS, LabelDTO.class);
        log.info("Successfully fetched {} labels from GitHub", labels.size());
        return labels;
    }

    public Set<MilestoneDTO> getMilestones() throws GitHubClientException {
        Set<MilestoneDTO> milestones = getEntities(GraphQLConstants.MILESTONES, MilestoneDTO.class);
        log.info("Successfully fetched {} milestones from GitHub", milestones.size());
        return milestones;
    }

    private <T> Set<T> getEntities(GraphQLConstants query, Class<T> entityType) throws GitHubClientException {
        Set<T> allEntities = new HashSet<>();
        String cursor = null;
        boolean hasNextPage = true;

        while (hasNextPage) {
            GraphQLDTO<T> response = fetchEntityPage(query, cursor, entityType);

            if (response == null || response.edges == null || response.edges.isEmpty()) {
                log.warn("Received empty response for query: {}", query);
                break;
            }

            Set<T> entities = response.edges.stream()
                    .map(edge -> objectMapper.convertValue(edge.node, entityType))
                    .collect(Collectors.toSet());

            allEntities.addAll(entities);

            log.info("Fetched {} entities for query: {}", entities.size(), query);

            hasNextPage = response.pageInfo != null && response.pageInfo.hasNextPage;
            cursor = (response.pageInfo != null) ? response.pageInfo.endCursor : null;
        }

        return allEntities;
    }

    private <T> GraphQLDTO<T> fetchEntityPage(GraphQLConstants query, String afterCursor, Class<T> entityType)
            throws GitHubClientException {
        try {
            GraphQLDTO<T> response = getGraphQlClient()
                    .documentName(query.getDocumentName())
                    .variable("after", afterCursor)
                    .retrieve(query.getRetrievePath())
                    .toEntity(new ParameterizedTypeReference<GraphQLDTO<T>>() {})
                    .block();

            log.info("Successfully executed paginated GraphQL query: {}", query);
            return response;
        } catch (Exception e) {
            throw new GitHubClientException("Failed to execute GraphQL request for " + query, e);
        }
    }

    private <T> T fetchSingleEntity(GraphQLConstants query, Class<T> entityType) throws GitHubClientException {
        try {
            T response = getGraphQlClient()
                    .documentName(query.getDocumentName())
                    .retrieve(query.getRetrievePath())
                    .toEntity(entityType)
                    .block();

            log.info("Successfully requested single object with GraphQL query: {}", query);
            return response;
        } catch (Exception e) {
            throw new GitHubClientException("Failed to execute GraphQL request for " + query, e);
        }
    }
}
