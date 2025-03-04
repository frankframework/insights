package org.frankframework.insights.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;

import lombok.Getter;

import java.util.Set;

@Entity
@Table(name = "milestone")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class Milestone {
	@Id
	private String id;

	@Column(nullable = false, unique = true)
	private String title;

	@OneToMany(mappedBy = "milestone")
	private Set<Issue> issues;
}

