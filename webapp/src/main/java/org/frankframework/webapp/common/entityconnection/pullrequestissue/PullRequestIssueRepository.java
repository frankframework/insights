package org.frankframework.webapp.common.entityconnection.pullrequestissue;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PullRequestIssueRepository extends JpaRepository<PullRequestIssue, UUID> {
    List<PullRequestIssue> findAllByPullRequest_Id(String pullRequestId);
}
