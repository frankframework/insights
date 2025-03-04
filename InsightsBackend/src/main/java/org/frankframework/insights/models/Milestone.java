package org.frankframework.insights.models;

import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "milestone")
public class Milestone {
	@Id
	private String id;

	@Column(nullable = false, unique = true)
	private String title;

	@OneToMany(mappedBy = "milestone")
	private Set<Issue> issues;
}

