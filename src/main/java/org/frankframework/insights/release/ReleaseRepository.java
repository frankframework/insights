package org.frankframework.insights.release;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReleaseRepository extends JpaRepository<Release, String> {
    Optional<Release> findByTagName(String tagName);
}
