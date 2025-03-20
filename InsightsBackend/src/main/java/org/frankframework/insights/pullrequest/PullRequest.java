package org.frankframework.insights.pullrequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Getter;
import org.frankframework.insights.common.entityconnection.PullRequestIssue;
import org.frankframework.insights.common.entityconnection.PullRequestLabel;
import org.frankframework.insights.milestone.Milestone;
import org.frankframework.insights.release.Release;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class PullRequest {
    @Id
    private String id;

    @Column(unique = true, nullable = false)
    private int githubId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String url;

    private LocalDateTime mergedAt;

    @ManyToOne
    private Milestone milestone;

    @ManyToOne
    private Release release;

    @OneToMany
    private Set<PullRequestIssue> pullRequestIssues;

    @OneToMany
    private Set<PullRequestLabel> pullRequestLabels;
}
