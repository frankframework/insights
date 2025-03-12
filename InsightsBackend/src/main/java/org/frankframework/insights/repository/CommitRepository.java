package org.frankframework.insights.repository;

import java.util.Set;
import org.frankframework.insights.models.Commit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CommitRepository extends JpaRepository<Commit, String> {
    @Query("SELECT c.oid FROM Commit c")
    Set<String> findAllOid();
}
