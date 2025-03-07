package org.frankframework.insights.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BranchDTO {
    @JsonProperty("id")
    public String id;

    @JsonProperty("name")
    public String name;
}
