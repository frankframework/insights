package org.frankframework.insights.release;

import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ReleaseRepository extends JpaRepository<Release, String> {
    @Query("SELECT DISTINCT r.name FROM Release r")
    Set<String> findAllNames();
}
