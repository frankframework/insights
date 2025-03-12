package org.frankframework.insights.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BranchDTO {
    @JsonProperty("id")
    public String id;

    @JsonProperty("name")
    public String name;
}
