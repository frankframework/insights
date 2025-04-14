package org.frankframework.insights.common.entityconnection.pullrequestlabel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PullRequestLabelRepository extends JpaRepository<PullRequestLabel, UUID> {
	List<PullRequestLabel> findAllByPullRequest_Id(String pullRequestId);
	List<PullRequestLabel> findAllByLabel_Id(String labelId);
}
