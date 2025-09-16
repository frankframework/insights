package org.frankframework.insights;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT2H")
@ConfigurationPropertiesScan
public class InsightsApplication {
    public static void main(String[] args) {
        SpringApplication app = configureApplication();
        app.run(args);
    }

    public static SpringApplication configureApplication() {
        return new SpringApplication(InsightsApplication.class);
    }
}
