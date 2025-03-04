package org.frankframework.insights.repository;

import java.util.Set;
import java.util.UUID;
import org.frankframework.insights.models.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LabelRepository extends JpaRepository<Label, String> {
}
