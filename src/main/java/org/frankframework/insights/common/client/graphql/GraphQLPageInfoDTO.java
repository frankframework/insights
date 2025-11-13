package org.frankframework.insights.common.client.graphql;

/**
 * A concrete DTO for GraphQL PageInfo.
 */
public record GraphQLPageInfoDTO(boolean hasNextPage, String endCursor) {}
