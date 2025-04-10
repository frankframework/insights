package org.frankframework.insights.release;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class ReleaseTagCommitDTO {
    @JsonAlias("oid")
    private String commitSha;

    public ReleaseTagCommitDTO(String commitSha) {
        this.commitSha = commitSha;
    }
}
