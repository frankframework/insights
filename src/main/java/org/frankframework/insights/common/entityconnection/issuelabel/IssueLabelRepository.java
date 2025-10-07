package org.frankframework.insights.common.entityconnection.issuelabel;

import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IssueLabelRepository extends JpaRepository<IssueLabel, IssueLabelId> {
    Set<IssueLabel> findAllByIssue_Id(String issueIds);

    Set<IssueLabel> findAllByIssue_IdIn(List<String> issueIds);
}
