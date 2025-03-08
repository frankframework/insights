package org.frankframework.insights.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CommitDTO {
    @JsonProperty("id")
    public String id;

    @JsonProperty("oid")
    public String oid;

    @JsonProperty("message")
    public String message;

    @JsonProperty("committedDate")
    public String committedDate;
}
