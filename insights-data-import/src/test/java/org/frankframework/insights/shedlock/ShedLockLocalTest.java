package org.frankframework.insights.shedlock;

import static org.mockito.Mockito.verifyNoInteractions;

import java.lang.reflect.Field;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockAssert;
import org.frankframework.insights.branch.BranchInjectionService;
import org.frankframework.insights.common.configuration.SystemDataInitializer;
import org.frankframework.insights.github.graphql.GitHubRepositoryStatisticsService;
import org.frankframework.insights.issue.IssueInjectionService;
import org.frankframework.insights.issueprojects.IssueProjectItemsInjectionService;
import org.frankframework.insights.issuetype.IssueTypeInjectionService;
import org.frankframework.insights.label.LabelInjectionService;
import org.frankframework.insights.milestone.MilestoneInjectionService;
import org.frankframework.insights.pullrequest.PullRequestInjectionService;
import org.frankframework.insights.release.ReleaseArtifactService;
import org.frankframework.insights.release.ReleaseInjectionService;
import org.frankframework.insights.vulnerability.VulnerabilityScanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("local")
@ExtendWith(MockitoExtension.class)
public class ShedLockLocalTest {
    @Mock
    private DataSource dataSource;

    @Mock
    private GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;

    @Mock
    private LabelInjectionService labelInjectionService;

    @Mock
    private MilestoneInjectionService milestoneInjectionService;

    @Mock
    private IssueTypeInjectionService issueTypeInjectionService;

    @Mock
    private IssueProjectItemsInjectionService issueProjectItemsInjectionService;

    @Mock
    private BranchInjectionService branchInjectionService;

    @Mock
    private IssueInjectionService issueInjectionService;

    @Mock
    private PullRequestInjectionService pullRequestInjectionService;

    @Mock
    private ReleaseInjectionService releaseInjectionService;

    @Mock
    private ReleaseArtifactService releaseArtifactService;

    @Mock
    private VulnerabilityScanService vulnerabilityScanService;

    @Mock
    private TaskExecutor taskExecutor;

    private SystemDataInitializer systemDataInitializer;

    @BeforeEach
    public void setUp() throws Exception {
        systemDataInitializer = new SystemDataInitializer(
                gitHubRepositoryStatisticsService,
                labelInjectionService,
                milestoneInjectionService,
                issueTypeInjectionService,
                issueProjectItemsInjectionService,
                branchInjectionService,
                issueInjectionService,
                pullRequestInjectionService,
                releaseInjectionService,
                releaseArtifactService,
                vulnerabilityScanService,
                taskExecutor);

        Field field = SystemDataInitializer.class.getDeclaredField("dataFetchEnabled");
        field.setAccessible(true);
        field.set(systemDataInitializer, false);

        LockAssert.TestHelper.makeAllAssertsPass(true);
    }

    @Test
    public void should_SkipGitHubFetch_when_LocalProfileIsActive() {
        systemDataInitializer.run();

        verifyNoInteractions(gitHubRepositoryStatisticsService);
        verifyNoInteractions(labelInjectionService);
        verifyNoInteractions(milestoneInjectionService);
        verifyNoInteractions(issueTypeInjectionService);
        verifyNoInteractions(issueProjectItemsInjectionService);
        verifyNoInteractions(branchInjectionService);
        verifyNoInteractions(issueInjectionService);
        verifyNoInteractions(pullRequestInjectionService);
        verifyNoInteractions(releaseInjectionService);
    }
}
