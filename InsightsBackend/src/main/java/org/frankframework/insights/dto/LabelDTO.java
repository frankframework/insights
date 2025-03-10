package org.frankframework.insights.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LabelDTO {
    @JsonProperty("id")
    public String id;

    @JsonProperty("name")
    public String name;

    @JsonProperty("description")
    public String description;

    @JsonProperty("color")
    public String color;
}
