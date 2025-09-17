package org.frankframework.insights.github;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GitHubQueryConstants {
    REPOSITORY_STATISTICS("repositoryStatistics", "repository"),
    LABELS("labels", "repository.labels"),
    MILESTONES("milestones", "repository.milestones"),
    ISSUE_TYPES("issueTypes", "repository.issueTypes"),
    ISSUE_PRIORITIES("issuePriorities", "node.fields"),
    BRANCHES("branches", "repository.refs"),
    ISSUES("issues", "repository.issues"),
    BRANCH_PULLS("branchPullRequests", "repository.pullRequests"),
    RELEASES("releases", "repository.releases");

    private final String documentName;
    private final String retrievePath;
}
