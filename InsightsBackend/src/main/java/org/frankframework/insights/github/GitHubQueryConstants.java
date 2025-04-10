package org.frankframework.insights.github;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GitHubQueryConstants {
    REPOSITORY_STATISTICS("repositoryStatistics", "repository"),
	BRANCH_STATISTICS("branchStatistics", "repository"),
	LABELS("labels", "repository.labels"),
    MILESTONES("milestones", "repository.milestones"),
    BRANCHES("branches", "repository.refs"),
    BRANCH_COMMITS("branchCommits", "repository.ref.target.history"),
	ISSUES("issues", "repository.issues"),
	BRANCH_PULLS("branchPullRequests", "repository.pullRequests"),
	RELEASES("releases", "repository.releases");

    private final String documentName;
    private final String retrievePath;
}
