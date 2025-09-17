package org.frankframework.insights.milestone;

import java.util.Set;
import org.frankframework.insights.github.GitHubPropertyState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilestoneRepository extends JpaRepository<Milestone, String> {
    Set<Milestone> findAllByState(GitHubPropertyState state);
}
