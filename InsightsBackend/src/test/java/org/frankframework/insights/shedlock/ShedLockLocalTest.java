package org.frankframework.insights.shedlock;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockAssert;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.common.configuration.SystemDataInitializer;
import org.frankframework.insights.common.configuration.properties.GitHubProperties;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.frankframework.insights.issue.IssueService;
import org.frankframework.insights.issuePriority.IssuePriorityService;
import org.frankframework.insights.issuetype.IssueTypeService;
import org.frankframework.insights.label.LabelService;
import org.frankframework.insights.milestone.MilestoneService;
import org.frankframework.insights.pullrequest.PullRequestService;
import org.frankframework.insights.release.ReleaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("local")
@ExtendWith(MockitoExtension.class)
public class ShedLockLocalTest {
    @Mock
    private DataSource dataSource;

    @Mock
    private GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;

    @Mock
    private LabelService labelService;

    @Mock
    private MilestoneService milestoneService;

    @Mock
    private IssueTypeService issueTypeService;

    @Mock
    private IssuePriorityService issuePriorityService;

    @Mock
    private BranchService branchService;

    @Mock
    private IssueService issueService;

    @Mock
    private PullRequestService pullRequestService;

    @Mock
    private ReleaseService releaseService;

    @Mock
    private GitHubProperties gitHubProperties;

    private SystemDataInitializer systemDataInitializer;

    @BeforeEach
    public void setUp() {
        when(gitHubProperties.getFetch()).thenReturn(false);

        systemDataInitializer = new SystemDataInitializer(
                gitHubRepositoryStatisticsService,
                labelService,
                milestoneService,
                issueTypeService,
                issuePriorityService,
                branchService,
                issueService,
                pullRequestService,
                releaseService,
                gitHubProperties);

        LockAssert.TestHelper.makeAllAssertsPass(true);
    }

    @Test
    public void should_SkipGitHubFetch_when_LocalProfileIsActive() {
        systemDataInitializer.run();

        verifyNoInteractions(gitHubRepositoryStatisticsService);
        verifyNoInteractions(labelService);
        verifyNoInteractions(milestoneService);
        verifyNoInteractions(issueTypeService);
        verifyNoInteractions(issuePriorityService);
        verifyNoInteractions(branchService);
        verifyNoInteractions(issueService);
        verifyNoInteractions(pullRequestService);
        verifyNoInteractions(releaseService);
    }
}
