package org.frankframework.insights.models;

import jakarta.persistence.*;

@Entity
@Table(name = "commit")
public class Commit {
	@Id
	private String id;

	@Column(nullable = false, unique = true)
	private String sha;

	@Column(nullable = false)
	private String message;

	@ManyToOne
	@JoinColumn(name = "pull_request_id")
	private PullRequest pullRequest;
}
