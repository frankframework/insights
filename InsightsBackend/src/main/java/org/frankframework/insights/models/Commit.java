package org.frankframework.insights.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "commit")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Commit {
	@Id
	private String id;

	@Column(nullable = false, unique = true)
	private String oid;

	@Lob
	@Column(nullable = false)
	private String message;

	@Column(name = "committed_date", columnDefinition = "TIMESTAMP")
	private OffsetDateTime committedDate;

	@ManyToMany(mappedBy = "commits", cascade = {CascadeType.MERGE, CascadeType.PERSIST})
	private Set<Branch> branches = new HashSet<>();

	@ManyToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
	private PullRequest pullRequest;
}
