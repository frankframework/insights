package org.frankframework.insights.common.configuration;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.frankframework.insights.common.configuration.properties.FetchProperties;
import org.frankframework.insights.snyk.SnykClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@Slf4j
public class SnykConfiguration implements CommandLineRunner {
    private final Boolean fetchEnabled;

    public SnykConfiguration(FetchProperties fetchProperties) {
        this.fetchEnabled = fetchProperties.getEnabled();
    }

    /**
     * CommandLineRunner method that runs on application startup.
     * @param args command line arguments
     */
    @Override
    @SchedulerLock(name = "startUpSnykUpdate", lockAtMostFor = "PT2H", lockAtLeastFor = "PT30M")
    public void run(String... args) {
        log.info("Startup: Fetching all Snyk data");
        initializeSnykData();
    }

    /**
     * Scheduled job that runs daily at midnight.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(name = "dailySnykUpdate", lockAtMostFor = "PT2H", lockAtLeastFor = "PT30M")
    public void dailyJob() {
        log.info("Daily fetch job started");
        initializeSnykData();
    }

    /**
     * Initializes data by fetching CVE'S from Snyk.io.
     */
    @SchedulerLock(name = "initializeSnykData", lockAtMostFor = "PT2H")
    public void initializeSnykData() {
        try {
            if (!fetchEnabled) {
                log.info("Skipping Snyk fetch: skipping due to build/test configuration.");
                return;
            }

            log.info("Start fetching all Snyk data");
            // todo add services with logic to fetch data from Snyk.io

            log.info("Done fetching all Snyk data");
        } catch (Exception e) {
            log.error("Error initializing Snyk data", e);
        }
    }
}
