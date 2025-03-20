package org.frankframework.insights.release;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReleaseDTO {
    public String id;

    public String tagName;

    public String name;

    public OffsetDateTime publishedAt;

    @JsonIgnore
    private ReleaseTagCommitDTO tagCommit;

    private String commitSha;

    @JsonSetter("tagCommit")
    public void setTagCommit(ReleaseTagCommitDTO tagCommit) {
        this.tagCommit = tagCommit;
        this.commitSha = tagCommit != null ? tagCommit.getCommitSha() : null;
    }
}
