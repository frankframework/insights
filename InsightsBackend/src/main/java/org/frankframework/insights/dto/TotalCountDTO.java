package org.frankframework.insights.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TotalCountDTO(@JsonProperty("totalCount") int totalCount) {}
