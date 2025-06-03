package org.frankframework.insights.issuePriority;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class IssuePriorityDTO {
	public String id;
	public String name;
	public String color;
	public String description;
}
