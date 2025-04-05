package org.frankframework.insights.issue;


import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueRepository extends JpaRepository<Issue, String> {
	List<Issue> findByParentIssue(Issue parent);
}
