package org.frankframework.webapp.label;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LabelRepository extends JpaRepository<Label, String> {
    @Query(
            """
     SELECT il.label
     FROM IssueLabel il
     WHERE il.issue.id IN (
      SELECT DISTINCT i.id
      FROM Issue i
      JOIN PullRequestIssue pri ON pri.issue = i
      JOIN ReleasePullRequest rpr ON rpr.pullRequest = pri.pullRequest
      WHERE rpr.release.id = :releaseId
      AND i.id NOT IN (SELECT s.id FROM Issue p JOIN p.subIssues s)
     )
  """)
    List<Label> findLabelsByReleaseId(@Param("releaseId") String releaseId);
}
