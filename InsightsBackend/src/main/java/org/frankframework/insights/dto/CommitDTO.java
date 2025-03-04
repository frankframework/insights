package org.frankframework.insights.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class CommitDTO {
	@JsonProperty("id")
	public String id;

	@JsonProperty("oid")
	public String sha;

	@JsonProperty("message")
	public String message;
}
