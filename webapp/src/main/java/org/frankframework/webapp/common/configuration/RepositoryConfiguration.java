package org.frankframework.webapp.common.configuration;

import org.frankframework.webapp.branch.BranchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Configuration
public class RepositoryConfiguration {

    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public BranchRepository branchRepository() {
        JpaRepositoryFactory factory = new JpaRepositoryFactory(entityManager);
        return factory.getRepository(BranchRepository.class);
    }
}