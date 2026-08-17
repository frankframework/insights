package org.frankframework.insights.issueprojects;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IssueStateRepository extends JpaRepository<IssueState, String> {}
