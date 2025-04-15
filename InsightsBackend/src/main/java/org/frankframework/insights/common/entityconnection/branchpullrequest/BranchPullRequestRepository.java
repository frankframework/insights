package org.frankframework.insights.common.entityconnection.branchpullrequest;

import java.util.Set;
import java.util.UUID;
import org.frankframework.insights.branch.Branch;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchPullRequestRepository extends JpaRepository<BranchPullRequest, UUID> {
    int countBranchPullRequestByBranch_Name(String name);

	@EntityGraph(attributePaths = {"pullRequest"})
	Set<BranchPullRequest> findAllByBranch_Id(String branchId);
}
