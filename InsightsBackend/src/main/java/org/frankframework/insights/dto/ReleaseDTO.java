package org.frankframework.insights.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class ReleaseDTO {
	@JsonProperty("id")
	public String id;

	@JsonProperty("tagName")
	public String tagName;

	@JsonProperty("name")
	public String name;

	@JsonProperty("publishedAt")
	public OffsetDateTime publishedAt;
}
