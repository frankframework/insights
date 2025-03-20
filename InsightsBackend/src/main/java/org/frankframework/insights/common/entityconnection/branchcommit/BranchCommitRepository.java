package org.frankframework.insights.common.entityconnection.branchcommit;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchCommitRepository extends JpaRepository<BranchCommit, UUID> {}
