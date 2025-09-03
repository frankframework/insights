package org.frankframework.insights.shedlock;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockAssert;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.common.configuration.GitHubConfiguration;
import org.frankframework.insights.common.configuration.SnykConfiguration;
import org.frankframework.insights.common.configuration.properties.FetchProperties;
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
    private FetchProperties fetchProperties;

    private GitHubConfiguration gitHubConfiguration;

	private SnykConfiguration snykConfiguration;

    // todo expand test classes with snyk client

    @BeforeEach
    public void setUp() {
        when(fetchProperties.getEnabled()).thenReturn(false);

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
    public void should_SkipGitHubFetch_when_LocalProfileIsActive() {
        gitHubConfiguration.run();

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

	@Test
	public void should_SkipSnykFetch_when_LocalProfileIsActive() {
		snykConfiguration.run();

		//todo add services here
//		verifyNoInteractions(vulnerabilityService);
	}
}
