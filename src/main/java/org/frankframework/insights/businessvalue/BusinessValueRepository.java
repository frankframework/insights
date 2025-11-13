package org.frankframework.insights.businessvalue;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessValueRepository extends JpaRepository<BusinessValue, UUID> {
    Optional<BusinessValue> findByName(String name);
}
