package org.frankframework.insights.pullrequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Set;
import lombok.Getter;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequest;
import org.frankframework.insights.common.entityconnection.PullRequestIssue;
import org.frankframework.insights.common.entityconnection.PullRequestLabel;
import org.frankframework.insights.milestone.Milestone;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class PullRequest {
    @Id
    private String id;

    @Column(unique = true, nullable = false)
    private int number;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String url;

    private OffsetDateTime mergedAt;

    @ManyToOne
    private Milestone milestone;

    @ManyToOne
    private Branch branch;

    @OneToMany
    private Set<BranchPullRequest> branchPullRequests;

    @OneToMany
    private Set<PullRequestIssue> pullRequestIssues;

    @OneToMany
    private Set<PullRequestLabel> pullRequestLabels;
}
