package org.frankframework.insights.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class TagCommitDTO {
    @JsonProperty("oid")
    private String oid;
}
