package org.frankframework.insights.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GraphQLConstants {
    LABELS("labels", "repository.labels"),
    MILESTONES("milestones", "repository.milestones");

    private final String documentName;
    private final String retrievePath;
}
