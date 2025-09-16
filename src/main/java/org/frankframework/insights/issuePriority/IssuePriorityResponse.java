package org.frankframework.insights.issuePriority;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IssuePriorityResponse(String id, String name, String color, String description) {}
