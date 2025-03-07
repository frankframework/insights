package org.frankframework.insights.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class BranchDTO {
    @JsonProperty("id")
    public String id;

    @JsonProperty("name")
    public String name;
}
