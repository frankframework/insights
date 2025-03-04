package org.frankframework.insights.configuration;

import org.frankframework.insights.service.LabelService;
import org.frankframework.insights.service.MilestoneService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
public class SystemDataInitializer {

    private final LabelService labelService;
    private final MilestoneService milestoneService;

    public SystemDataInitializer(LabelService labelService, MilestoneService milestoneService) {
        this.labelService = labelService;
        this.milestoneService = milestoneService;
    }

    @Scheduled(initialDelay = 1000, fixedRate = Long.MAX_VALUE)
    public void InitializeSystemData() throws RuntimeException {
        labelService.injectLabels();
        milestoneService.injectMilestones();
    }
}
