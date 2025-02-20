package org.frankframework.insights.models;

import jakarta.persistence.*;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "milestone")
public class Milestone {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true)
	private String title;

	@Column(name = "major_version", nullable = false)
	private String majorVersion;

	@Column(nullable = false)
	private int openIssues;

	@Column(nullable = false)
	private int closedIssues;

	@OneToMany(mappedBy = "milestone")
	private Set<Issue> issues;
}
