package org.frankframework.insights.common.client.graphql;

import java.util.List;

/**
 * Represents a generic GraphQL "Connection" for cursor-based pagination.
 * It holds a list of edges and the pagination information.
 * @param <T> the type of the entity within the connection's nodes.
 */
public record GraphQLConnectionDTO<T>(List<GraphQLNodeDTO<T>> edges, GraphQLPageInfoDTO pageInfo) {}
