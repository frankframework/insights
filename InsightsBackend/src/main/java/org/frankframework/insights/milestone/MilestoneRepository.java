package org.frankframework.insights.milestone;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MilestoneRepository extends JpaRepository<Milestone, String> {
	List<Milestone> getMilestonesByState(String state);
}
