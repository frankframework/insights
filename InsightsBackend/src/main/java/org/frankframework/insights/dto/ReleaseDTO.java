package org.frankframework.insights.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.Getter;

public class ReleaseDTO {
    @JsonProperty("id")
    public String id;

	@Getter
    @JsonProperty("tagName")
    public String tagName;

    @JsonProperty("name")
    public String name;

    @JsonProperty("publishedAt")
    public OffsetDateTime publishedAt;

    @Getter
    @JsonProperty("tagCommit")
    private TagCommitDTO tagCommit;
}
