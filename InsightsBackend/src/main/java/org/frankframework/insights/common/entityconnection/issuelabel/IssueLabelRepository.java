package org.frankframework.insights.common.entityconnection.issuelabel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface IssueLabelRepository extends JpaRepository<IssueLabel, UUID> {
	Set<IssueLabel> findAllByIssue_Id(String issueId);
	Set<IssueLabel> findAllByLabel_Id(String labelId);
}
