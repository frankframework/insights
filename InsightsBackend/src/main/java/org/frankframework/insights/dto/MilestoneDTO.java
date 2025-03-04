package org.frankframework.insights.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MilestoneDTO {
    @JsonProperty("id")
    public String id;

    @JsonProperty("title")
    public String title;

    @JsonProperty("description")
    public String description;
}
