package org.frankframework.insights.repository;

import org.frankframework.insights.models.Commit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface CommitRepository extends JpaRepository<Commit, String> {
	@Query("SELECT c.oid FROM Commit c")
	Set<String> findAllOid();
}
