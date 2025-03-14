package org.frankframework.insights.release;

import org.frankframework.insights.branch.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReleaseRepository extends JpaRepository<Release, String> {
    List<Release> findByBranchOrderByPublishedAtAsc(Branch branch);
}
