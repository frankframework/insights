package org.frankframework.insights.release;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReleaseDTO {
    @JsonProperty("id")
    public String id;

    @JsonProperty("tagName")
    public String tagName;

    @JsonProperty("name")
    public String name;

    @JsonProperty("publishedAt")
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
