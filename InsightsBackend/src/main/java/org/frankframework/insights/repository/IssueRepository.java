package org.frankframework.insights.repository;

import java.util.UUID;
import org.frankframework.insights.models.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueRepository extends JpaRepository<Issue, UUID> {}
