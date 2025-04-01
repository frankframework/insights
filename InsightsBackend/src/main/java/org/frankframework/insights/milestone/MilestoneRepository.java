package org.frankframework.insights.milestone;

import org.frankframework.insights.github.GitHubPropertyState;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MilestoneRepository extends JpaRepository<Milestone, String> {
	List<Milestone> getMilestonesByState(GitHubPropertyState state);
}
