package org.frankframework.insights.release;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ReleaseRepository extends JpaRepository<Release, String> {
    @EntityGraph(attributePaths = {"releaseCommits", "releaseCommits.commit"})
    @Query("SELECT r FROM Release r")
    List<Release> findAllWithCommits();
}
