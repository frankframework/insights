package org.frankframework.insights.common.entityconnection.pullrequestlabel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PullRequestLabelRepository extends JpaRepository<PullRequestLabel, PullRequestLabelId> {}
