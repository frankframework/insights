package org.frankframework.insights.common.entityconnection;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.frankframework.insights.issue.Issue;
import org.frankframework.insights.pullrequest.PullRequest;

@Entity
@Getter
@Setter
public class PullRequestIssue {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private PullRequest pullRequest;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Issue issue;
}
