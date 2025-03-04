package org.frankframework.insights.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.frankframework.insights.configuration.GitHubProperties;
import org.frankframework.insights.dto.GraphQLDTO;
import org.frankframework.insights.dto.LabelDTO;
import org.frankframework.insights.dto.MilestoneDTO;
import org.frankframework.insights.dto.ReleaseDTO;
import org.frankframework.insights.service.GraphQLQueryService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class GitHubClient extends ApiClient {

	private final GraphQLQueryService graphQLQueryService;
	private final ObjectMapper objectMapper;

	public GitHubClient(
			GitHubProperties gitHubProperties,
			GraphQLQueryService graphQLQueryService,
			ObjectMapper objectMapper) {
		super(gitHubProperties.getUrl(), gitHubProperties.getSecret());
		this.graphQLQueryService = graphQLQueryService;
		this.objectMapper = objectMapper;
	}

	public Set<LabelDTO> getLabels() {
		return getGitHubGraphQLEntities(0, LabelDTO.class, "labels");
	}

	public Set<MilestoneDTO> getMilestones() {
		return getGitHubGraphQLEntities(1, MilestoneDTO.class, "milestones");
	}

	public Set<ReleaseDTO> getReleases() {
		return getGitHubGraphQLEntities(2, ReleaseDTO.class, "releases");
	}

	private <T> Set<T> getGitHubGraphQLEntities(int queryIndex, Class<T> entityType, String entityName) {
		Set<T> allEntities = new HashSet<>();
		String cursor = null;
		boolean hasNextPage = true;

		while (hasNextPage) {
			GraphQLDTO<T> response = fetchEntityPage(queryIndex, cursor, entityType);

			if (response == null || response.data == null || response.data.repository == null) {
				break;
			}

			GraphQLDTO.EntityConnection<T> entityConnection = response.data.repository.getEntities().get(entityName);
			if (entityConnection == null || entityConnection.edges == null) {
				break;
			}

			allEntities.addAll(entityConnection.edges.stream()
					.map(edge -> objectMapper.convertValue(edge.node, entityType))
					.collect(Collectors.toSet()));

			hasNextPage = entityConnection.pageInfo != null && entityConnection.pageInfo.hasNextPage;
			cursor = entityConnection.pageInfo != null ? entityConnection.pageInfo.endCursor : null;
		}

		return allEntities;
	}

	private <T> GraphQLDTO<T> fetchEntityPage(int queryIndex, String afterCursor, Class<T> entityType) {
		JsonNode query = graphQLQueryService.customizeQuery(queryIndex, afterCursor, null, null);
		return request(query, new ParameterizedTypeReference<GraphQLDTO<T>>() {});
	}
}
