package org.frankframework.webapp.common.entityconnection.pullrequestissue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.frankframework.webapp.issue.Issue;
import org.frankframework.webapp.pullrequest.PullRequest;

@Entity
@Getter
@Setter
@NoArgsConstructor
@IdClass(PullRequestIssueId.class)
public class PullRequestIssue {
    @Id
    @ManyToOne
    @JoinColumn(nullable = false)
    @JsonIgnore
    private PullRequest pullRequest;

    @Id
    @ManyToOne(cascade = {CascadeType.MERGE})
    @JoinColumn(nullable = false)
    private Issue issue;

    public PullRequestIssue(PullRequest pullRequest, Issue issue) {
        this.pullRequest = pullRequest;
        this.issue = issue;
    }
}
