package org.frankframework.insights.issue;

import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IssueRepository extends JpaRepository<Issue, String> {
	/**
	 * This query involves multiple joins across different entities. For such complex
	 * cases, a custom @Query is more readable and maintainable than a very long
	 * derived method name.
	 */
	@Query(
			"""
   SELECT DISTINCT i
   FROM Issue i
   JOIN PullRequestIssue pri ON pri.issue = i
   JOIN ReleasePullRequest rpr ON rpr.pullRequest = pri.pullRequest
   WHERE rpr.release.id = :releaseId
""")
	Set<Issue> findIssuesByReleaseId(@Param("releaseId") String releaseId);

	/**
	 * Finds all distinct issues by traversing the milestone relationship and matching its ID.
	 * Spring Data JPA generates the query from this method name.
	 */
	Set<Issue> findDistinctByMilestoneId(String milestoneId);

	Set<Issue> findIssuesByIssueTypeNameAndMilestoneIsNull(String typeName);
}
