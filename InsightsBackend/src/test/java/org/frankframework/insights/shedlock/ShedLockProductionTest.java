package org.frankframework.insights.shedlock;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockAssert;
import org.frankframework.insights.branch.BranchInjectionException;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.common.configuration.SystemDataInitializer;
import org.frankframework.insights.common.configuration.properties.GitHubProperties;
import org.frankframework.insights.github.GitHubClientException;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.frankframework.insights.issue.IssueInjectionException;
import org.frankframework.insights.issue.IssueService;
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
                    PullRequestInjectionException {
        systemDataInitializer.run();

        verify(gitHubRepositoryStatisticsService, times(1)).fetchRepositoryStatistics();
        verify(labelService, times(1)).injectLabels();
        verify(milestoneService, times(1)).injectMilestones();
        verify(branchService, times(1)).injectBranches();
        verify(issueService, times(1)).injectIssues();
        verify(pullRequestService, times(1)).injectBranchPullRequests();
        verify(releaseService, times(1)).injectReleases();
    }
}
