package org.frankframework.insights.issueprojects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IssueStateResponse(String id, String name, String color, String description) {}
