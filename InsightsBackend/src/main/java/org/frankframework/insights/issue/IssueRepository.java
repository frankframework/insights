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
     AND i.id NOT IN (SELECT s.id FROM Issue p JOIN p.subIssues s)
""")
    Set<Issue> findRootIssuesByReleaseId(@Param("releaseId") String releaseId);

    @Query(
            """
   SELECT DISTINCT i
   FROM Issue i
   WHERE i.milestone.id = :milestoneId
     AND i.id NOT IN (SELECT s.id FROM Issue p JOIN p.subIssues s)
""")
    Set<Issue> findRootIssuesByMilestoneId(@Param("milestoneId") String milestoneId);

    @Query(
            """
   SELECT DISTINCT i
   FROM Issue i
   WHERE i.closedAt BETWEEN :start AND :end
     AND i.id NOT IN (SELECT s.id FROM Issue p JOIN p.subIssues s)
""")
    Set<Issue> findRootIssuesByClosedAtBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
}
