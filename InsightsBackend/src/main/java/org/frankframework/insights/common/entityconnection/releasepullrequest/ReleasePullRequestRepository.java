package org.frankframework.insights.common.entityconnection.releasepullrequest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReleasePullRequestRepository extends JpaRepository<ReleasePullRequest, UUID> {
	List<ReleasePullRequest> findAllByRelease_Id(String id);
}
