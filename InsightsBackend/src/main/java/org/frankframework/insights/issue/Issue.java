package org.frankframework.insights.issue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.Set;
import lombok.Getter;
import org.frankframework.insights.common.entityconnection.IssueLabel;
import org.frankframework.insights.common.entityconnection.PullRequestIssue;
import org.frankframework.insights.github.GitHubPropertyState;
import org.frankframework.insights.milestone.Milestone;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class Issue {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private int number;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private GitHubPropertyState state;

    @Column(nullable = false)
    private String url;

    @OneToMany
    private Set<IssueLabel> issueLabels;

    @ManyToOne
    private Milestone milestone;

    @OneToMany(mappedBy = "parentIssue")
    private Set<Issue> subIssues;

    @ManyToOne
    private Issue parentIssue;

    @OneToMany
    private Set<PullRequestIssue> pullRequestIssues;
}
