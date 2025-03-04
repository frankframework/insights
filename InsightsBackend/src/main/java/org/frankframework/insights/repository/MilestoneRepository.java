package org.frankframework.insights.repository;

import java.util.UUID;
import org.frankframework.insights.models.Issue;
import org.frankframework.insights.models.Milestone;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MilestoneRepository extends JpaRepository<Milestone, UUID> {}
