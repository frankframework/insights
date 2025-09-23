package org.frankframework.webapp.shedlock;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockAssert;
import org.frankframework.webapp.branch.BranchService;
import org.frankframework.webapp.common.configuration.SystemDataInitializer;
import org.frankframework.webapp.common.configuration.properties.GitHubProperties;
import org.frankframework.webapp.github.GitHubRepositoryStatisticsService;
import org.frankframework.webapp.issue.IssueService;
import org.frankframework.webapp.issuePriority.IssuePriorityService;
import org.frankframework.webapp.issuetype.IssueTypeService;
import org.frankframework.webapp.label.LabelService;
import org.frankframework.webapp.milestone.MilestoneService;
import org.frankframework.webapp.pullrequest.PullRequestService;
import org.frankframework.webapp.release.ReleaseService;
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
