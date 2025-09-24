package org.frankframework.webapp.common.configuration;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan(basePackages = {
    "org.frankframework.webapp",
    "org.frankframework.cve_scanner",
    "org.frankframework.shared"
})
@EnableJpaRepositories(basePackages = {
    "org.frankframework.webapp",
    "org.frankframework.cve_scanner",
    "org.frankframework.shared"
})
@EntityScan(basePackages = {
    "org.frankframework.webapp",
    "org.frankframework.cve_scanner",
    "org.frankframework.shared"
})
public class JpaConfiguration {
}