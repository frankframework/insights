package org.frankframework.insights.commit;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CommitDTO {
    @JsonProperty("id")
    public String id;

    @JsonAlias("oid")
    public String sha;

    @JsonProperty("message")
    public String message;

    @JsonProperty("committedDate")
    public String committedDate;
}
