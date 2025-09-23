package org.frankframework.webapp.issuetype;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IssueTypeResponse(String id, String name, String description, String color) {}
