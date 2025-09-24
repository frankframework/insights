package org.frankframework.webapp.common.configuration;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import org.frankframework.cve_scanner.ScanOrchestrator;
import org.frankframework.webapp.common.configuration.properties.GitHubProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@Slf4j
public class NexusConfiguration implements CommandLineRunner {
	private final ScanOrchestrator scanOrchestrator;
    private final Boolean fetchEnabled;

    public NexusConfiguration(ScanOrchestrator scanOrchestrator, GitHubProperties gitHubProperties) {
		this.scanOrchestrator = scanOrchestrator;
		this.fetchEnabled = gitHubProperties.getFetch();
    }

    /**
     * CommandLineRunner method that runs on application startup.
     * @param args command line arguments
     */
    @Override
    @SchedulerLock(name = "startUpVulnerabilityUpdate", lockAtMostFor = "PT2H", lockAtLeastFor = "PT30M")
    public void run(String... args) {
        log.info("Startup: Fetching vulnerability data");
        initializeVulnerabilityData();
    }

    /**
     * Scheduled job that runs daily at midnight.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(name = "dailyGitHubUpdate", lockAtMostFor = "PT2H", lockAtLeastFor = "PT30M")
    public void dailyJob() {
        log.info("Daily fetch job started");
        initializeVulnerabilityData();
    }

    /**
     * Initializes vulnerability data by fetching dependencies and vulnerabilities from nexus.
     */
    @SchedulerLock(name = "initializeVulnerabilityData", lockAtMostFor = "PT2H")
    public void initializeVulnerabilityData() {
        try {
            if (!fetchEnabled) {
                log.info("Skipping Vulnerability fetch: skipping due to build/test configuration.");
                return;
            }

            log.info("Start fetching all vulnerability data");
            scanOrchestrator.executeDailyScan();

            log.info("Done fetching all vulnerability data");
        } catch (Exception e) {
            log.error("Error initializing system data", e);
        }
    }
}
