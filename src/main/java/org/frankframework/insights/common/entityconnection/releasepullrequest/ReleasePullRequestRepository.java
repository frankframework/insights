package org.frankframework.insights.common.entityconnection.releasepullrequest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReleasePullRequestRepository extends JpaRepository<ReleasePullRequest, ReleasePullRequestId> {}
