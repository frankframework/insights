package org.frankframework.insights.common.entityconnection.releasecommit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReleaseCommitRepository extends JpaRepository<ReleaseCommit, UUID> {
	List<ReleaseCommit> findAllByRelease_Id(String releaseId);
}
