package org.frankframework.insights.configuration;

import org.frankframework.insights.repository.ReleaseRepository;
import org.frankframework.insights.service.CommitService;
import org.frankframework.insights.service.MilestoneService;
import org.frankframework.insights.service.LabelService;
import org.frankframework.insights.service.ReleaseService;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
public class SystemDataInitializer {

    private final LabelService labelService;
    private final MilestoneService milestoneService;
	private final ReleaseService releaseService;
	private final CommitService commitService;

    public SystemDataInitializer(LabelService labelService, MilestoneService milestoneService, ReleaseService releaseService, CommitService commitService) {
        this.labelService = labelService;
        this.milestoneService = milestoneService;
		this.releaseService = releaseService;
		this.commitService = commitService;
    }

	@Scheduled(initialDelay = 1000, fixedRate = Long.MAX_VALUE)
    public void InitializeSystemData() throws RuntimeException {
        labelService.injectLabels();
        milestoneService.injectMilestones();
		releaseService.injectReleases();
		commitService.injectCommits();
    }
}
