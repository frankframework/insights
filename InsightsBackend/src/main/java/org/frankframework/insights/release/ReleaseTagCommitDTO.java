package org.frankframework.insights.release;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReleaseTagCommitDTO {
	@JsonAlias("oid")
	private String commitSha;
}
