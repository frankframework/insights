package org.frankframework.insights.common.entityconnection.pullrequestlabel;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PullRequestLabelRepository extends JpaRepository<PullRequestLabel, PullRequestLabelId> {

    @Modifying
    @Transactional
    void deleteAllByPullRequest_IdIn(List<String> pullRequestIds);
}
