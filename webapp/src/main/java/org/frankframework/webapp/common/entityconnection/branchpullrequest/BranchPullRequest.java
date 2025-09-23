package org.frankframework.webapp.common.entityconnection.branchpullrequest;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.frankframework.webapp.branch.Branch;
import org.frankframework.webapp.pullrequest.PullRequest;

@Entity
@Getter
@Setter
@NoArgsConstructor
@IdClass(BranchPullRequestId.class)
public class BranchPullRequest {
    @Id
    @ManyToOne
    @JoinColumn(nullable = false)
    private Branch branch;

    @Id
    @ManyToOne(cascade = {CascadeType.MERGE})
    @JoinColumn(nullable = false)
    private PullRequest pullRequest;

    public BranchPullRequest(Branch branch, PullRequest pullRequest) {
        this.branch = branch;
        this.pullRequest = pullRequest;
    }
}
