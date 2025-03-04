package org.frankframework.insights.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "pull_request")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class PullRequest {
	@Id
	private String id;

	@Column(name = "github_id", unique = true, nullable = false)
	private int githubId;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String url;

	@Column(name = "merged_at", columnDefinition = "TIMESTAMP")
	private LocalDateTime mergedAt;

	@ManyToOne
	@JoinColumn(name = "milestone_id")
	private Milestone milestone;

	@ManyToOne
	@JoinColumn(name = "release_id")
	private Release release;

	@ManyToOne
	@JoinColumn(name = "issue_id")
	private Issue issue;

	@ManyToMany
	@JoinTable(
			name = "pull_request_label",
			joinColumns = @JoinColumn(name = "pull_request_id"),
			inverseJoinColumns = @JoinColumn(name = "label_id"))
	private Set<Label> labels;
}
