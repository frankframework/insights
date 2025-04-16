package org.frankframework.insights.common.entityconnection.branchcommit;

import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchCommitRepository extends JpaRepository<BranchCommit, UUID> {
    int countBranchCommitByBranch_Name(String name);

    @EntityGraph(attributePaths = {"commit"})
    Set<BranchCommit> findAllByBranch_Id(String branchId);
}
