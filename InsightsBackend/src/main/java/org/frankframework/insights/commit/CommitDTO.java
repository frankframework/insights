package org.frankframework.insights.commit;

import com.fasterxml.jackson.annotation.JsonAlias;

public class CommitDTO {
    public String id;

    @JsonAlias("oid")
    public String sha;

    public String message;

    public String committedDate;
}
