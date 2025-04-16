package org.frankframework.insights.common.entityconnection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.pullrequest.PullRequest;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
public class BranchPullRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(nullable = false)
    @JsonIgnore
    private Branch branch;

    @ManyToOne
    @JoinColumn(nullable = false)
    private PullRequest pullRequest;

    public BranchPullRequest(Branch branch, PullRequest pullRequest) {
        this.branch = branch;
        this.pullRequest = pullRequest;
    }
}
