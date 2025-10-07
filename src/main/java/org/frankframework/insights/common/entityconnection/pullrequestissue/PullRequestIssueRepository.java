package org.frankframework.insights.common.entityconnection.pullrequestissue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PullRequestIssueRepository extends JpaRepository<PullRequestIssue, PullRequestIssueId> { }
