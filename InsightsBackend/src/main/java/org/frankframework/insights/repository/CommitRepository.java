package org.frankframework.insights.repository;

import org.frankframework.insights.models.Commit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommitRepository extends JpaRepository<Commit, String> {
}
