package org.frankframework.insights.common.entityconnection.branchcommit;

import java.util.Set;
import java.util.UUID;
import org.frankframework.insights.branch.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchCommitRepository extends JpaRepository<BranchCommit, UUID> {
    int countBranchCommitByBranch_Name(String name);

    Set<BranchCommit> findAllByBranch_Id(String branchId);
}
