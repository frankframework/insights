package org.frankframework.insights.issuePriority;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.insights.issue.Issue;

import java.util.Set;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class IssuePriority {
	@Id
	private String id;

	@Column(nullable = false, unique = true)
	private String name;

	@Column(nullable = false)
	private String color;

	private String description;

	@OneToMany(mappedBy = "issuePriority")
	@JsonManagedReference("issuePriority-issue")
	private Set<Issue> issues;

}
