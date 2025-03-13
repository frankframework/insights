package org.frankframework.insights.common.configuration;

import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.label.LabelInjectionException;
import org.frankframework.insights.milestone.MilestoneInjectionException;
import org.frankframework.insights.label.LabelService;
import org.frankframework.insights.milestone.MilestoneService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@Slf4j
public class SystemDataInitializer {

    private final LabelService labelService;
    private final MilestoneService milestoneService;

    public SystemDataInitializer(LabelService labelService, MilestoneService milestoneService) {
        this.labelService = labelService;
        this.milestoneService = milestoneService;
    }

    @Scheduled(initialDelay = 1000, fixedRate = Long.MAX_VALUE)
    public void initializeSystemData() throws LabelInjectionException, MilestoneInjectionException {
        log.info("Start fetching all GitHub data");
        labelService.injectLabels();
        milestoneService.injectMilestones();
        log.info("Done fetching all GitHub data");
    }
}
