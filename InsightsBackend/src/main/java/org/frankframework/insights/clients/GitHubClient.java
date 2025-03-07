package org.frankframework.insights.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.frankframework.insights.configuration.GitHubProperties;
import org.frankframework.insights.dto.BranchDTO;
import org.frankframework.insights.dto.CommitDTO;
import org.frankframework.insights.dto.GraphQLDTO;
import org.frankframework.insights.dto.LabelDTO;
import org.frankframework.insights.dto.MilestoneDTO;
import org.frankframework.insights.dto.ReleaseDTO;
import org.frankframework.insights.enums.GraphQLConstants;
import org.frankframework.insights.exceptions.clients.GitHubClientException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class GitHubClient extends GraphQLClient {
    private final ObjectMapper objectMapper;

    public GitHubClient(GitHubProperties gitHubProperties, ObjectMapper objectMapper) {
        super(gitHubProperties.getUrl(), gitHubProperties.getSecret());
        this.objectMapper = objectMapper;
    }

    public Set<LabelDTO> getLabels() throws GitHubClientException {
        try {
            return getEntities(GraphQLConstants.LABELS, new HashMap<>(), LabelDTO.class);
        } catch (Exception e) {
            throw new GitHubClientException();
        }
    }

    public Set<MilestoneDTO> getMilestones() throws GitHubClientException {
        try {
            return getEntities(GraphQLConstants.MILESTONES, new HashMap<>(), MilestoneDTO.class);
        } catch (Exception e) {
            throw new GitHubClientException();
        }
    }

    public Set<BranchDTO> getBranches() throws GitHubClientException {
        try {
            return getEntities(GraphQLConstants.BRANCHES, new HashMap<>(), BranchDTO.class);
        } catch (Exception e) {
            throw new GitHubClientException();
        }
    }

    public Set<CommitDTO> getBranchCommits(String branchName) throws GitHubClientException {
        try {
            HashMap<String, Object> variables = new HashMap<>() {
                {
                    put("branchName", branchName);
                }
            };

            return getEntities(GraphQLConstants.BRANCH_COMMITS, variables, CommitDTO.class);
        } catch (Exception e) {
            throw new GitHubClientException();
        }
    }

    public Set<ReleaseDTO> getReleases() throws GitHubClientException {
        try {
            return getEntities(GraphQLConstants.RELEASES, new HashMap<>(), ReleaseDTO.class);
        } catch (Exception e) {
            throw new GitHubClientException();
        }
    }

    private <T> Set<T> getEntities(GraphQLConstants query, Map<String, Object> queryVariables, Class<T> entityType)
            throws GitHubClientException {
        Set<T> allEntities = new HashSet<>();
        String cursor = null;
        boolean hasNextPage = true;

        while (hasNextPage) {
            queryVariables.put("after", cursor);

            GraphQLDTO<T> response = fetchEntityPage(query, queryVariables, entityType);

            allEntities.addAll(response.edges.stream()
                    .map(edge -> objectMapper.convertValue(edge.node, entityType))
                    .collect(Collectors.toSet()));

            hasNextPage = response.pageInfo != null && response.pageInfo.hasNextPage;
            cursor = response.pageInfo != null ? response.pageInfo.endCursor : null;
        }

        return allEntities;
    }

    private <T> GraphQLDTO<T> fetchEntityPage(
            GraphQLConstants query, Map<String, Object> queryVariables, Class<T> entityType)
            throws GitHubClientException {
        try {
            return getGraphQlClient()
                    .documentName(query.getDocumentName())
                    .variables(queryVariables)
                    .retrieve(query.getRetrievePath())
                    .toEntity(new ParameterizedTypeReference<GraphQLDTO<T>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            throw new GitHubClientException();
        }
    }
}
