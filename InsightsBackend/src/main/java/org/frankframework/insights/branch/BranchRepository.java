package org.frankframework.insights.branch;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchRepository extends JpaRepository<Branch, String> {
	Branch findBranchByName(String name);

	@EntityGraph(attributePaths = {"branchCommits.commit"})
	@Query("SELECT b FROM Branch b")
	List<Branch> findAllWithCommits();

	@EntityGraph(attributePaths = {"branchPullRequests.pullRequest"})
	@Query("SELECT b FROM Branch b WHERE b IN :branches")
	List<Branch> findAllWithPullRequests(@Param("branches") List<Branch> branches);
}
