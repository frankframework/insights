package org.frankframework.insights.milestone;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MilestoneDTO {
    @JsonProperty("id")
    public String id;

    @JsonProperty("title")
    public String title;
}
