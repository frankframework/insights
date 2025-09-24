package org.frankframework.webapp;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT2H")
@ConfigurationPropertiesScan
@EntityScan(basePackages = {
		"org.frankframework.webapp",
		"org.frankframework.cve_scanner",
		"org.frankframework.shared"
})
public class InsightsApplication {
    public static void main(String[] args) {
        SpringApplication app = configureApplication();
        app.run(args);
    }

    public static SpringApplication configureApplication() {
        return new SpringApplication(InsightsApplication.class);
    }
}
