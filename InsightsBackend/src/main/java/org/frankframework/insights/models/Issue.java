package org.frankframework.insights.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.util.Set;
import java.util.UUID;
import lombok.Setter;

@Entity
@Table(name = "issue")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Issue {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonProperty("number")
    @Column(name = "github_id", nullable = false, unique = true)
    private int githubId;

    @JsonProperty("title")
    @Column(nullable = false)
    private String title;

    @JsonProperty("url")
    @Column(nullable = false)
    private String url;

    @ManyToOne
    @JoinColumn(name = "milestone_id")
    private Milestone milestone;

    @ManyToOne
    @JoinColumn(name = "type_id")
    private Type type;

    @Setter
    @ManyToMany
    @JoinTable(
            name = "issue_label",
            joinColumns = @JoinColumn(name = "issue_id"),
            inverseJoinColumns = @JoinColumn(name = "label_id"))
    private Set<Label> labels;

    @Column(name = "pull_requests")
    @OneToMany(mappedBy = "issue")
    private Set<PullRequest> pullRequests;
}
