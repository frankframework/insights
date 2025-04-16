package org.frankframework.insights.issue;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueRepository extends JpaRepository<Issue, String> {
	List<Issue> findByParentIssue(Issue parent);
}
