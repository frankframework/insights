package org.frankframework.insights.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Getter;

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
    private Milestone milestone;

    @ManyToOne
    private Release release;

    @ManyToMany
    @JoinTable(
            name = "pull_request_issue",
            joinColumns = @JoinColumn(name = "pull_request_id"),
            inverseJoinColumns = @JoinColumn(name = "issue_id"))
    private Set<Issue> issues;

    @ManyToMany
    @JoinTable(
            name = "pull_request_label",
            joinColumns = @JoinColumn(name = "pull_request_id"),
            inverseJoinColumns = @JoinColumn(name = "label_id"))
    private Set<Label> labels;
}
