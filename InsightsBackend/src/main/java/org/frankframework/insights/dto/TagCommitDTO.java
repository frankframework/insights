package org.frankframework.insights.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TagCommitDTO {
    @JsonProperty("oid")
    private String oid;
}
