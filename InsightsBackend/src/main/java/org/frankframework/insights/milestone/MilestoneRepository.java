package org.frankframework.insights.milestone;

import java.util.List;
import org.frankframework.insights.github.GitHubPropertyState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilestoneRepository extends JpaRepository<Milestone, String> {
    List<Milestone> getMilestonesByState(GitHubPropertyState state);
}
