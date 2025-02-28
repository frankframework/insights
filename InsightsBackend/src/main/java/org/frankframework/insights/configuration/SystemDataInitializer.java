package org.frankframework.insights.configuration;

import org.frankframework.insights.service.IssueService;
import org.frankframework.insights.service.LabelService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
public class SystemDataInitializer {

    private final LabelService labelService;
    private final IssueService issueService;

    public SystemDataInitializer(LabelService labelService, IssueService issueService) {
        this.labelService = labelService;
        this.issueService = issueService;
    }

	@Scheduled(initialDelay = 1000, fixedRate = Long.MAX_VALUE)
    public void InitializeSystemData() throws RuntimeException {
        labelService.injectLabels();
        issueService.injectIssues();
    }
}
