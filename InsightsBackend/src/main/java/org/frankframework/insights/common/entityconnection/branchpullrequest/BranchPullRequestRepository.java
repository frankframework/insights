package org.frankframework.insights.common.entityconnection.branchpullrequest;

import org.frankframework.insights.branch.Branch;

import org.frankframework.insights.common.entityconnection.branchcommit.BranchCommit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;
import java.util.UUID;

public interface BranchPullRequestRepository extends JpaRepository<BranchPullRequest, UUID> {
	int countBranchPullRequestByBranch(Branch branch);
	Set<BranchPullRequest> findBranchPullRequestByBranchId(String branchId);
}
