package org.frankframework.insights.issue;

import java.time.OffsetDateTime;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IssueRepository extends JpaRepository<Issue, String> {
    Set<Issue> findAllByMilestone_Id(String milestoneId);

    Set<Issue> findAllByClosedAtBetween(OffsetDateTime startDate, OffsetDateTime endDate);
}
