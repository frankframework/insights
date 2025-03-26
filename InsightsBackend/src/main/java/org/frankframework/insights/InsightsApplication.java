package org.frankframework.insights;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT2H")
public class InsightsApplication {
    public static void main(String[] args) {
        SpringApplication.run(InsightsApplication.class, args);
    }
}
