package org.frankframework.webapp.common.entityconnection.releasepullrequest;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReleasePullRequestRepository extends JpaRepository<ReleasePullRequest, UUID> {
    List<ReleasePullRequest> findAllByRelease_Id(String id);
}
