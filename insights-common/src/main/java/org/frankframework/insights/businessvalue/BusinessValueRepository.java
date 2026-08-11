package org.frankframework.insights.businessvalue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.frankframework.insights.release.Release;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessValueRepository extends JpaRepository<BusinessValue, UUID> {
    Optional<BusinessValue> findByTitleAndRelease(String title, Release release);

    List<BusinessValue> findByReleaseId(String releaseId);
}
