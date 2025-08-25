package org.frankframework.insights.issue;

import java.time.OffsetDateTime;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IssueRepository extends JpaRepository<Issue, String> {
    @Query(
            """
   SELECT DISTINCT i
   FROM Issue i
   JOIN PullRequestIssue pri ON pri.issue = i
   JOIN ReleasePullRequest rpr ON rpr.pullRequest = pri.pullRequest
   WHERE rpr.release.id = :releaseId
""")
    Set<Issue> findIssuesByReleaseId(@Param("releaseId") String releaseId);

    @Query(
            """
   SELECT DISTINCT i
   FROM Issue i
   WHERE i.milestone.id = :milestoneId
""")
    Set<Issue> findIssuesByMilestoneId(@Param("milestoneId") String milestoneId);

    @Query(
            """
   SELECT DISTINCT i
   FROM Issue i
   WHERE i.closedAt BETWEEN :start AND :end
""")
    Set<Issue> findIssuesByClosedAtBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
}
