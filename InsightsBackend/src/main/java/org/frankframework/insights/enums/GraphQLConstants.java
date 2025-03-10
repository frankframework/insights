package org.frankframework.insights.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GraphQLConstants {
    LABELS("labels", "repository.labels"),
    MILESTONES("milestones", "repository.milestones"),
    BRANCHES("branches", "repository.refs"),
    BRANCH_COMMITS("branchCommits", "repository.ref.target.history"),
    RELEASES("releases", "repository.releases");

    private final String documentName;
    private final String retrievePath;
}
