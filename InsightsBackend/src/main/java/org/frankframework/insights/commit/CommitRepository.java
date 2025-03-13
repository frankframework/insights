package org.frankframework.insights.commit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CommitRepository extends JpaRepository<Commit, String> {
	@Query("SELECT COUNT(bc) FROM Branch b JOIN b.commits bc")
	int countAllBranchCommitRelations();
}
