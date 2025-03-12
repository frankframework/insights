package org.frankframework.insights.configuration;

import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.exceptions.branches.BranchInjectionException;
import org.frankframework.insights.exceptions.commits.CommitInjectionException;
import org.frankframework.insights.exceptions.labels.LabelInjectionException;
import org.frankframework.insights.exceptions.milestones.MilestoneInjectionException;
import org.frankframework.insights.exceptions.releases.ReleaseInjectionException;
import org.frankframework.insights.service.BranchService;
import org.frankframework.insights.service.CommitService;
import org.frankframework.insights.service.LabelService;
import org.frankframework.insights.service.MilestoneService;
import org.frankframework.insights.service.ReleaseService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@Slf4j
public class SystemDataInitializer {

    private final LabelService labelService;
    private final MilestoneService milestoneService;
    private final BranchService branchService;
    private final CommitService commitService;
    private final ReleaseService releaseService;

    public SystemDataInitializer(
            LabelService labelService,
            MilestoneService milestoneService,
            BranchService branchService,
            CommitService commitService,
            ReleaseService releaseService) {
        this.labelService = labelService;
        this.milestoneService = milestoneService;
        this.branchService = branchService;
        this.commitService = commitService;
        this.releaseService = releaseService;
    }

    @Scheduled(initialDelay = 1000, fixedRate = Long.MAX_VALUE)
    public void InitializeSystemData()
            throws LabelInjectionException, MilestoneInjectionException, BranchInjectionException,
                    CommitInjectionException, ReleaseInjectionException {
        log.info("Start fetching all GitHub data");
        labelService.injectLabels();
        milestoneService.injectMilestones();
        branchService.injectBranches();
        commitService.injectBranchCommits();
        releaseService.injectReleases();
        log.info("Done fetching all GitHub data");
    }
}
