package org.frankframework.insights.shedlock;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockAssert;
import org.frankframework.insights.branch.BranchInjectionException;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.common.configuration.SystemDataInitializer;
import org.frankframework.insights.github.graphql.GitHubGraphQLClientException;
import org.frankframework.insights.github.graphql.GitHubRepositoryStatisticsService;
import org.frankframework.insights.issue.IssueInjectionException;
import org.frankframework.insights.issue.IssueService;
import org.frankframework.insights.issueprojects.IssueProjectItemInjectionException;
import org.frankframework.insights.issueprojects.IssueProjectItemsService;
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
import org.frankframework.insights.vulnerability.VulnerabilityService;
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
    private IssueProjectItemsService issueProjectItemsService;

    @Mock
    private BranchService branchService;

    @Mock
    private IssueService issueService;

    @Mock
    private PullRequestService pullRequestService;

    @Mock
    private ReleaseService releaseService;

    @Mock
    private VulnerabilityService vulnerabilityService;

    private SystemDataInitializer systemDataInitializer;

    @BeforeEach
    public void setUp() throws Exception {
        systemDataInitializer = new SystemDataInitializer(
                gitHubRepositoryStatisticsService,
                labelService,
                milestoneService,
                issueTypeService,
                issueProjectItemsService,
                branchService,
                issueService,
                pullRequestService,
                releaseService,
                vulnerabilityService);

        Field field = SystemDataInitializer.class.getDeclaredField("dataFetchEnabled");
        field.setAccessible(true);
        field.set(systemDataInitializer, true);

        LockAssert.TestHelper.makeAllAssertsPass(true);
    }

    @Test
    public void should_FetchGitHubData_when_ProductionProfileIsActive()
            throws LabelInjectionException, GitHubGraphQLClientException, MilestoneInjectionException,
                    BranchInjectionException, ReleaseInjectionException, IssueInjectionException,
                    PullRequestInjectionException, IssueTypeInjectionException, IssueProjectItemInjectionException {
        systemDataInitializer.run();

        verify(gitHubRepositoryStatisticsService, times(1)).fetchRepositoryStatistics();
        verify(labelService, times(1)).injectLabels();
        verify(milestoneService, times(1)).injectMilestones();
        verify(issueTypeService, times(1)).injectIssueTypes();
        verify(issueProjectItemsService, times(1)).injectIssueProjectItems();
        verify(branchService, times(1)).injectBranches();
        verify(issueService, times(1)).injectIssues();
        verify(pullRequestService, times(1)).injectBranchPullRequests();
        verify(releaseService, times(1)).injectReleases();
    }
}
