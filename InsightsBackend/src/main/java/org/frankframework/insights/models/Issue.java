package org.frankframework.insights.models;

import jakarta.persistence.*;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "issue")
public class Issue {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "github_id", nullable = false, unique = true)
	private int githubId;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String url;

	@ManyToOne
	@JoinColumn(name = "milestone_id")
	private Milestone milestone;

	@ManyToOne
	@JoinColumn(name = "type_id")
	private Type type;

	@ManyToMany
	@JoinTable(
			name = "issue_label",
			joinColumns = @JoinColumn(name = "issue_id"),
			inverseJoinColumns = @JoinColumn(name = "label_id")
	)
	private Set<Label> labels;

	@Column(name = "pull_requests")
	@OneToMany(mappedBy = "issue")
	private Set<PullRequest> pullRequests;
}
