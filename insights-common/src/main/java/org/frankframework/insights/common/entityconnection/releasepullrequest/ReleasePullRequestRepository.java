package org.frankframework.insights.common.entityconnection.releasepullrequest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ReleasePullRequestRepository extends JpaRepository<ReleasePullRequest, ReleasePullRequestId> {

    @Modifying
    @Transactional
    void deleteAllByReleaseId(String releaseId);
}
