package org.frankframework.insights;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InsightsApplication {
    public static void main(String[] args) {
        SpringApplication.run(InsightsApplication.class, args);
    }
}
