package org.frankframework.insights.shedlock;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.common.configuration.GitHubConfiguration;
import org.frankframework.insights.common.configuration.ShedLockConfiguration;
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

@ExtendWith(MockitoExtension.class)
public class ShedLockTest {

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

    @BeforeEach
    public void setUp() {
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

        LockAssert.TestHelper.makeAllAssertsPass(true);
    }

    // todo add tests 'what if github and snyk run through eachother + db lock?'
    // todo add snyk tests

    @Test
    public void should_CreateLockProvider_when_BeanIsInitialized() {
        ShedLockConfiguration shedLockConfiguration = new ShedLockConfiguration();
        JdbcTemplateLockProvider lockProvider = shedLockConfiguration.lockProvider(dataSource);

        assertNotNull(lockProvider, "LockProvider bean should be created");
    }

    @Test
    public void should_LockStartupTask_when_Executed() {
        gitHubConfiguration.run();
        LockAssert.assertLocked();
    }

    @Test
    public void should_LockDailyJob_when_Executed() {
        gitHubConfiguration.dailyJob();
        LockAssert.assertLocked();
    }

    @Test
    public void should_NotAllowStartupTaskToInterrupt_when_DailyJobIsRunning() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        Future<?> startupFuture = executorService.submit(() -> {
            latch.countDown();
            gitHubConfiguration.run();
        });

        Future<?> dailyJobFuture = executorService.submit(() -> {
            try {
                latch.await();
                gitHubConfiguration.dailyJob();
            } catch (InterruptedException e) {
                throw new RuntimeException("Thread was interrupted while waiting", e);
            }
        });

        startupFuture.get();
        LockAssert.assertLocked();

        try {
            dailyJobFuture.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof InterruptedException) {
                assertNotNull(e.getCause());
                assertInstanceOf(InterruptedException.class, e.getCause());
            }
        }
    }

    @Test
    public void should_NotAllowDailyJobToInterrupt_when_StartupTaskIsRunning() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        Future<?> dailyJobFuture = executorService.submit(() -> {
            try {
                latch.await();
                gitHubConfiguration.dailyJob();
            } catch (InterruptedException e) {
                throw new RuntimeException("Thread was interrupted while waiting", e);
            }
        });

        Future<?> startupFuture = executorService.submit(() -> {
            latch.countDown();
            gitHubConfiguration.run();
        });

        dailyJobFuture.get();
        LockAssert.assertLocked();

        try {
            startupFuture.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof InterruptedException) {
                assertNotNull(e.getCause());
                assertInstanceOf(InterruptedException.class, e.getCause());
            }
        }
    }
}
