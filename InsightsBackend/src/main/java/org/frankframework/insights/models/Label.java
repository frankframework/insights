package org.frankframework.insights.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "label")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Label {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

	@JsonProperty("name")
    @Column(unique = true, nullable = false)
    private String name;

	@JsonProperty("color")
	@Column(nullable = false)
	private String color;

    @ManyToMany(mappedBy = "labels")
    private Set<Issue> issues;

    @ManyToMany(mappedBy = "labels")
    private Set<PullRequest> pullRequests;
}
