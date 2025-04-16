package org.frankframework.insights.common.entityconnection.pullrequestlabel;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PullRequestLabelRepository extends JpaRepository<PullRequestLabel, UUID> {
    List<PullRequestLabel> findAllByPullRequest_Id(String pullRequestId);

    List<PullRequestLabel> findAllByLabel_Id(String labelId);
}
