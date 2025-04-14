package org.frankframework.insights.common.entityconnection.pullrequestissue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PullRequestIssueRepository extends JpaRepository<PullRequestIssue, UUID> {
	List<PullRequestIssue> findAllByPullRequest_Id(String pullRequestId);
}
