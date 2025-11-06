package org.frankframework.insights.github.graphql;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.frankframework.insights.common.client.graphql.GraphQLQuery;

import org.frankframework.insights.common.client.graphql.GraphQLQuery;

@Getter
@AllArgsConstructor
public enum GitHubQueryConstants implements GraphQLQuery {
    REPOSITORY_STATISTICS("repositoryStatistics", "repository"),
    LABELS("labels", "repository.labels"),
    MILESTONES("milestones", "repository.milestones"),
    ISSUE_TYPES("issueTypes", "repository.issueTypes"),
    ISSUE_PROJECT_ITEMS("issueProjects", "node.fields"),
    BRANCHES("branches", "repository.refs"),
    ISSUES("issues", "repository.issues"),
    BRANCH_PULLS("branchPullRequests", "repository.pullRequests"),
    RELEASES("releases", "repository.releases");

    private final String documentName;
    private final String retrievePath;
}
