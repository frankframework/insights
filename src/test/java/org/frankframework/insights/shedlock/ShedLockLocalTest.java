package org.frankframework.insights.shedlock;

import static org.mockito.Mockito.verifyNoInteractions;

import java.lang.reflect.Field;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockAssert;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.common.configuration.SystemDataInitializer;
import org.frankframework.insights.github.graphql.GitHubRepositoryStatisticsService;
import org.frankframework.insights.issue.IssueService;
import org.frankframework.insights.issueprojects.IssueProjectItemsService;
import org.frankframework.insights.issuetype.IssueTypeService;
import org.frankframework.insights.label.LabelService;
import org.frankframework.insights.milestone.MilestoneService;
import org.frankframework.insights.pullrequest.PullRequestService;
import org.frankframework.insights.release.ReleaseArtifactService;
import org.frankframework.insights.release.ReleaseService;
import org.frankframework.insights.vulnerability.VulnerabilityService;
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
    private ReleaseArtifactService releaseArtifactService;

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
                releaseArtifactService,
                vulnerabilityService);

        Field field = SystemDataInitializer.class.getDeclaredField("dataFetchEnabled");
        field.setAccessible(true);
        field.set(systemDataInitializer, false);

        LockAssert.TestHelper.makeAllAssertsPass(true);
    }

    @Test
    public void should_SkipGitHubFetch_when_LocalProfileIsActive() {
        systemDataInitializer.run();

        verifyNoInteractions(gitHubRepositoryStatisticsService);
        verifyNoInteractions(labelService);
        verifyNoInteractions(milestoneService);
        verifyNoInteractions(issueTypeService);
        verifyNoInteractions(issueProjectItemsService);
        verifyNoInteractions(branchService);
        verifyNoInteractions(issueService);
        verifyNoInteractions(pullRequestService);
        verifyNoInteractions(releaseService);
    }
}
