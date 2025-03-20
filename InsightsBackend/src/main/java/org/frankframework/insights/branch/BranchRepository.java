package org.frankframework.insights.branch;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchRepository extends JpaRepository<Branch, String> {
    @EntityGraph(attributePaths = {"branchCommits", "branchCommits.commit"})
    @Query("SELECT b FROM Branch b")
    List<Branch> findAllWithCommits();
}
