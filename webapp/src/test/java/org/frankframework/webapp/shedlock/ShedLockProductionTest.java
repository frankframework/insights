package org.frankframework.webapp.shedlock;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockAssert;
import org.frankframework.webapp.branch.BranchInjectionException;
import org.frankframework.webapp.branch.BranchService;
import org.frankframework.webapp.common.configuration.SystemDataInitializer;
import org.frankframework.webapp.common.configuration.properties.GitHubProperties;
import org.frankframework.webapp.github.GitHubClientException;
import org.frankframework.webapp.github.GitHubRepositoryStatisticsService;
import org.frankframework.webapp.issue.IssueInjectionException;
import org.frankframework.webapp.issue.IssueService;
import org.frankframework.webapp.issuePriority.IssuePriorityInjectionException;
import org.frankframework.webapp.issuePriority.IssuePriorityService;
import org.frankframework.webapp.issuetype.IssueTypeInjectionException;
import org.frankframework.webapp.issuetype.IssueTypeService;
import org.frankframework.webapp.label.LabelInjectionException;
import org.frankframework.webapp.label.LabelService;
import org.frankframework.webapp.milestone.MilestoneInjectionException;
import org.frankframework.webapp.milestone.MilestoneService;
import org.frankframework.webapp.pullrequest.PullRequestInjectionException;
import org.frankframework.webapp.pullrequest.PullRequestService;
import org.frankframework.webapp.release.ReleaseInjectionException;
import org.frankframework.webapp.release.ReleaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("production")
@ExtendWith(MockitoExtension.class)
public class ShedLockProductionTest {
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
        when(gitHubProperties.getFetch()).thenReturn(true);

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
    public void should_FetchGitHubData_when_ProductionProfileIsActive()
            throws LabelInjectionException, GitHubClientException, MilestoneInjectionException,
                    BranchInjectionException, ReleaseInjectionException, IssueInjectionException,
                    PullRequestInjectionException, IssueTypeInjectionException, IssuePriorityInjectionException {
        systemDataInitializer.run();

        verify(gitHubRepositoryStatisticsService, times(1)).fetchRepositoryStatistics();
        verify(labelService, times(1)).injectLabels();
        verify(milestoneService, times(1)).injectMilestones();
        verify(issueTypeService, times(1)).injectIssueTypes();
        verify(issuePriorityService, times(1)).injectIssuePriorities();
        verify(branchService, times(1)).injectBranches();
        verify(issueService, times(1)).injectIssues();
        verify(pullRequestService, times(1)).injectBranchPullRequests();
        verify(releaseService, times(1)).injectReleases();
    }
}
