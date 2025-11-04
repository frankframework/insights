package org.frankframework.insights.businessvalue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BusinessValueRepository extends JpaRepository<BusinessValue, UUID> {
}
