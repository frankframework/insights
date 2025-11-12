package org.frankframework.insights.common.client.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.client.ApiClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
public abstract class GraphQLClient extends ApiClient {
    private final HttpGraphQlClient graphQlClient;
    private final ObjectMapper objectMapper;

    public GraphQLClient(String baseUrl, Consumer<WebClient.Builder> configurer, ObjectMapper objectMapper) {
        super(baseUrl, configurer);
        this.graphQlClient = HttpGraphQlClient.builder(this.webClient).build();
        this.objectMapper = objectMapper;
    }

    protected <T> T fetchSingleEntity(GraphQLQuery query, Map<String, Object> queryVariables, Class<T> entityType)
            throws GraphQLClientException {
        try {
            return getGraphQlClient()
                    .documentName(query.getDocumentName())
                    .variables(queryVariables)
                    .retrieve(query.getRetrievePath())
                    .toEntity(entityType)
                    .block();
        } catch (Exception e) {
            throw new GraphQLClientException("Failed GraphQL request for document: " + query.getDocumentName(), e);
        }
    }

    /**
     * A flexible method to fetch paginated data, allowing the caller to define how collections are extracted.
     * This can handle various GraphQL response structures, such as those with 'edges' or 'nodes'.
     *
     * @param query the GraphQL query constant.
     * @param queryVariables the variables for the query.
     * @param entityType the class type of the final entities.
     * @param responseType the type reference for deserializing the raw GraphQL response.
     * @param collectionExtractor a function to extract the collection of raw data maps from the response.
     * @param pageInfoExtractor a function to extract pagination info from the response.
     * @return a set of all fetched entities across all pages.
     * @throws GraphQLClientException if the request fails.
     */
    protected <RAW, T> Set<T> fetchPaginatedCollection(
            GraphQLQuery query,
            Map<String, Object> queryVariables,
            Class<T> entityType,
            ParameterizedTypeReference<RAW> responseType,
            Function<RAW, Collection<Map<String, Object>>> collectionExtractor,
            Function<RAW, GraphQLPageInfoDTO> pageInfoExtractor)
            throws GraphQLClientException {

        Function<RAW, Collection<T>> entityExtractor = response -> {
            Collection<Map<String, Object>> rawNodes = collectionExtractor.apply(response);
            if (rawNodes == null) {
                return Set.of();
            }
            return rawNodes.stream()
                    .map(node -> objectMapper.convertValue(node, entityType))
                    .collect(Collectors.toList());
        };

        return fetchPaginated(query, queryVariables, responseType, entityExtractor, pageInfoExtractor);
    }

    private <RAW, T> Set<T> fetchPaginated(
            GraphQLQuery query,
            Map<String, Object> queryVariables,
            ParameterizedTypeReference<RAW> responseType,
            Function<RAW, Collection<T>> entityExtractor,
            Function<RAW, GraphQLPageInfoDTO> pageInfoExtractor)
            throws GraphQLClientException {
        try {
            Set<T> allEntities = new HashSet<>();
            String cursor = null;
            boolean hasNextPage = true;

            while (hasNextPage) {
                queryVariables.put("after", cursor);
                RAW response = getGraphQlClient()
                        .documentName(query.getDocumentName())
                        .variables(queryVariables)
                        .retrieve(query.getRetrievePath())
                        .toEntity(responseType)
                        .block();

                if (response == null) {
                    log.warn("Received null response for query: {}", query);
                    break;
                }

                Collection<T> entities = entityExtractor.apply(response);
                if (entities == null || entities.isEmpty()) {
                    log.warn("Received empty entities for query: {}", query);
                    break;
                }

                allEntities.addAll(entities);
                log.info("Fetched {} entities with query: {}", entities.size(), query);

                GraphQLPageInfoDTO pageInfo = pageInfoExtractor.apply(response);
                hasNextPage = pageInfo != null && pageInfo.hasNextPage();
                cursor = (pageInfo != null) ? pageInfo.endCursor() : null;
            }
            log.info(
                    "Completed paginated fetch for [{}], total entities found: {}",
                    query.getDocumentName(),
                    allEntities.size());
            return allEntities;
        } catch (Exception e) {
            throw new GraphQLClientException(
                    "Failed paginated GraphQL request for document: " + query.getDocumentName(), e);
        }
    }

    protected HttpGraphQlClient getGraphQlClient() {
        return graphQlClient;
    }
}
