package org.frankframework.insights.common.entityconnection.releasedependency;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleaseDependencyRepository extends JpaRepository<ReleaseDependency, UUID> {}
