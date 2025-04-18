package org.frankframework.insights.issue;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IssueRepository extends JpaRepository<Issue, String> {
    List<Issue> findByParentIssue(Issue parent);
}
