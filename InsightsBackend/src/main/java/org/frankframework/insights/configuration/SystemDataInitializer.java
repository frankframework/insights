package org.frankframework.insights.configuration;

import jakarta.annotation.PostConstruct;
import org.frankframework.insights.service.IssueService;
import org.frankframework.insights.service.LabelService;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SystemDataInitializer {

    private final LabelService labelService;
    private final IssueService issueService;

    public SystemDataInitializer(LabelService labelService, IssueService issueService) {
        this.labelService = labelService;
        this.issueService = issueService;
    }

    @PostConstruct
    public void InitializeSystemData() throws RuntimeException {
        labelService.injectLabels();
        issueService.injectIssues();
    }
}
