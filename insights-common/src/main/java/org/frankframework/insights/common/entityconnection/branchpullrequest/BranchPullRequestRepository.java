package org.frankframework.insights.common.entityconnection.branchpullrequest;

import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchPullRequestRepository extends JpaRepository<BranchPullRequest, BranchPullRequestId> {
    int countAllByBranch_Id(String branchId);

    @EntityGraph(attributePaths = {"pullRequest"})
    Set<BranchPullRequest> findAllByBranch_Id(String branchId);
}
