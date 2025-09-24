package org.frankframework.shared.repository;

import java.util.Set;
import org.frankframework.shared.entity.Release;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReleaseRepository extends JpaRepository<Release, String> {
    Set<String> findAllTagNames();
}
