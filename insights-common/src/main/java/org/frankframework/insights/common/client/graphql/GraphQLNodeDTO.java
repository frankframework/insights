package org.frankframework.insights.common.client.graphql;

/**
 * A generic container for a "node" in a GraphQL edge, part of the Relay cursor-based pagination spec.
 * @param <T> the type of the entity contained within the node.
 */
public record GraphQLNodeDTO<T>(T node) {}
