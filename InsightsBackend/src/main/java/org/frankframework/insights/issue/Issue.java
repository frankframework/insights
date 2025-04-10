package org.frankframework.insights.issue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.frankframework.insights.common.entityconnection.IssueLabel;
import org.frankframework.insights.common.entityconnection.PullRequestIssue;
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

    @Column(nullable = false)
    private String url;

    @Lob
    private String businessValue;

    @OneToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    private Set<IssueLabel> issueLabels;

    @ManyToOne(cascade = CascadeType.MERGE)
    private Milestone milestone;

    @ManyToOne
    private Issue parentIssue;

    @OneToMany
    @JsonIgnore
    private Set<Issue> subIssues;

    @OneToMany
    private Set<PullRequestIssue> pullRequestIssues;
}
