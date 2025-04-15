package org.frankframework.insights.common.entityconnection.pullrequestissue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.frankframework.insights.issue.Issue;
import org.frankframework.insights.pullrequest.PullRequest;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
public class PullRequestIssue {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private PullRequest pullRequest;

    @ManyToOne(cascade = {CascadeType.MERGE})
    @JoinColumn(nullable = false)
    private Issue issue;

    public PullRequestIssue(PullRequest pullRequest, Issue issue) {
        this.pullRequest = pullRequest;
        this.issue = issue;
    }
}
