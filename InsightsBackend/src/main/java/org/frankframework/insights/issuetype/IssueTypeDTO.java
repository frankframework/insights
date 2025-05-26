package org.frankframework.insights.issuetype;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class IssueTypeDTO {
	public String id;
	public String name;
	public String description;
	public String color;
}
