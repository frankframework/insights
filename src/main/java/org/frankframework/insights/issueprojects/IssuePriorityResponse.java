package org.frankframework.insights.issueprojects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IssuePriorityResponse(String id, String name, String color, String description) {}
