package org.frankframework.insights.issue;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.frankframework.insights.github.GitHubPropertyState;
import org.frankframework.insights.milestone.Milestone;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Issue {
    @Id
    private String id;

    @Column(nullable = false)
    private int number;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private GitHubPropertyState state;

    private OffsetDateTime closedAt;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false, length = 2000)
    private String businessValue;

    @ManyToOne(cascade = CascadeType.MERGE)
    @JsonBackReference
    private Milestone milestone;

    @ManyToOne
    private Issue parentIssue;

    @OneToMany
    @JsonIgnore
    private Set<Issue> subIssues;
}
