package org.frankframework.insights.common.entityconnection.pullrequestissue;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PullRequestIssueRepository extends JpaRepository<PullRequestIssue, PullRequestIssueId> {

    @Modifying
    @Transactional
    void deleteAllByPullRequest_IdIn(List<String> pullRequestIds);
}
