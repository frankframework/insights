package org.frankframework.insights;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point of the Insights data import service.
 * <p>
 * This module gathers data from external sources (GitHub and Trivy) and writes it to the Insights
 * database. It exposes no read API; serving that data is the responsibility of the separate
 * {@code insights-webapp} module. The only HTTP endpoints it publishes are the GitHub release
 * webhook that triggers a refresh, and the actuator health/info endpoints.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableSchedulerLock(defaultLockAtMostFor = "PT2H", proxyTargetClass = true)
@ConfigurationPropertiesScan
public class InsightsDataImportApplication {
    public static void main(String[] args) {
        SpringApplication app = configureApplication();
        app.run(args);
    }

    public static SpringApplication configureApplication() {
        return new SpringApplication(InsightsDataImportApplication.class);
    }
}
