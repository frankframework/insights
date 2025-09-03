package org.frankframework.insights.shedlock;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockAssert;
import org.frankframework.insights.branch.BranchInjectionException;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.common.configuration.GitHubConfiguration;
import org.frankframework.insights.common.configuration.SnykConfiguration;
import org.frankframework.insights.common.configuration.properties.FetchProperties;
import org.frankframework.insights.github.GitHubClientException;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.frankframework.insights.issue.IssueInjectionException;
import org.frankframework.insights.issue.IssueService;
import org.frankframework.insights.issuePriority.IssuePriorityInjectionException;
import org.frankframework.insights.issuePriority.IssuePriorityService;
import org.frankframework.insights.issuetype.IssueTypeInjectionException;
import org.frankframework.insights.issuetype.IssueTypeService;
import org.frankframework.insights.label.LabelInjectionException;
import org.frankframework.insights.label.LabelService;
import org.frankframework.insights.milestone.MilestoneInjectionException;
import org.frankframework.insights.milestone.MilestoneService;
import org.frankframework.insights.pullrequest.PullRequestInjectionException;
import org.frankframework.insights.pullrequest.PullRequestService;
import org.frankframework.insights.release.ReleaseInjectionException;
import org.frankframework.insights.release.ReleaseService;
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
    private FetchProperties fetchProperties;

    private GitHubConfiguration gitHubConfiguration;

	private SnykConfiguration snykConfiguration;

    @BeforeEach
    public void setUp() {
        when(fetchProperties.getEnabled()).thenReturn(true);

        gitHubConfiguration = new GitHubConfiguration(
                gitHubRepositoryStatisticsService,
                labelService,
                milestoneService,
                issueTypeService,
                issuePriorityService,
                branchService,
                issueService,
                pullRequestService,
                releaseService,
                fetchProperties);

		snykConfiguration = new SnykConfiguration(
				fetchProperties
		);

        LockAssert.TestHelper.makeAllAssertsPass(true);
    }

    @Test
    public void should_FetchGitHubData_when_ProductionProfileIsActive()
            throws LabelInjectionException, GitHubClientException, MilestoneInjectionException,
                    BranchInjectionException, ReleaseInjectionException, IssueInjectionException,
                    PullRequestInjectionException, IssueTypeInjectionException, IssuePriorityInjectionException {
        gitHubConfiguration.run();

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

	@Test
	public void should_FetchSnykData_when_ProductionProfileIsActive() {
		snykConfiguration.run();

		verify(vulnerabilityService, times(1)).injectVulnerabilities();
	}
}
