package org.frankframework.insights.repository;

import org.frankframework.insights.models.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilestoneRepository extends JpaRepository<Milestone, String> {}
