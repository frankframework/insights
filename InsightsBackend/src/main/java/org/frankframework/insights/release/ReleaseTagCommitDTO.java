package org.frankframework.insights.release;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReleaseTagCommitDTO {
    @JsonProperty("oid")
    private String oid;
}
