package org.frankframework.insights.common.configuration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.frankframework.insights.branch.BranchInjectionService;
import org.frankframework.insights.github.graphql.GitHubGraphQLClientException;
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
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@ExtendWith(MockitoExtension.class)
public class SystemDataInitializerTest {

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
    private ApplicationContext appContext;

    private final TaskExecutor taskExecutor = new SyncTaskExecutor();

    private SystemDataInitializer systemDataInitializer;

    @BeforeEach
    public void setUp() {
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
                taskExecutor,
                appContext);

        systemDataInitializer.dataFetchEnabled = true;
    }

    @Test
    public void triggerRefresh_whenNoJobRunning_startsWork() {
        systemDataInitializer.triggerRefresh();

        verify(vulnerabilityScanService).scanUnscannedReleasesOnly();
    }

    @Test
    public void triggerRefresh_whenJobAlreadyRunning_queuesRefreshWithoutRunningImmediately() throws Exception {
        setJobRunning(true);

        systemDataInitializer.triggerRefresh();

        verify(releaseInjectionService, never()).injectReleases();
        verify(vulnerabilityScanService, never()).scanUnscannedReleasesOnly();
    }

    @Test
    public void triggerRefresh_alwaysRunsFullInject() throws Exception {
        systemDataInitializer.triggerRefresh();

        verify(labelInjectionService).injectLabels();
        verify(milestoneInjectionService).injectMilestones();
        verify(issueTypeInjectionService).injectIssueTypes();
        verify(issueProjectItemsInjectionService).injectIssueProjectItems();
        verify(branchInjectionService).injectBranches();
        verify(issueInjectionService).injectIssues();
        verify(pullRequestInjectionService).injectBranchPullRequests();
        verify(releaseInjectionService).injectReleases();
        verify(releaseArtifactService).deleteObsoleteReleaseArtifacts();
    }

    @Test
    public void triggerRefresh_alwaysScansUnscannedReleasesOnly_notFullRescan() {
        systemDataInitializer.triggerRefresh();

        verify(vulnerabilityScanService).scanUnscannedReleasesOnly();
        verify(vulnerabilityScanService, never()).scanAndSaveVulnerabilitiesForAllReleases();
    }

    @Test
    public void triggerRefresh_whenInjectThrows_logsErrorAndResetsLock() throws Exception {
        doThrow(new RuntimeException("GitHub unreachable"))
                .when(labelInjectionService)
                .injectLabels();

        systemDataInitializer.triggerRefresh();

        reset(labelInjectionService);
        systemDataInitializer.triggerRefresh();
        verify(vulnerabilityScanService, times(2)).scanUnscannedReleasesOnly();
    }

    @Test
    public void triggerRefresh_whenEarlierStepThrows_laterStepsIncludingReleaseInjectionStillRun() throws Exception {
        doThrow(new RuntimeException("GitHub unreachable"))
                .when(labelInjectionService)
                .injectLabels();

        systemDataInitializer.triggerRefresh();

        verify(milestoneInjectionService).injectMilestones();
        verify(issueProjectItemsInjectionService).injectIssueProjectItems();
        verify(branchInjectionService).injectBranches();
        verify(releaseInjectionService).injectReleases();
        verify(releaseArtifactService).deleteObsoleteReleaseArtifacts();
        verify(vulnerabilityScanService).scanUnscannedReleasesOnly();
    }

    @Test
    public void triggerRefresh_whenReleaseInjectionItselfThrows_laterStepsStillRunAndScanStillHappens()
            throws Exception {
        doThrow(new RuntimeException("GitHub unreachable"))
                .when(releaseInjectionService)
                .injectReleases();

        systemDataInitializer.triggerRefresh();

        verify(releaseArtifactService).deleteObsoleteReleaseArtifacts();
        verify(vulnerabilityScanService).scanUnscannedReleasesOnly();
    }

    @Test
    public void triggerRefresh_whenScanThrows_logsErrorAndResetsLock() {
        doThrow(new RuntimeException("Trivy unavailable"))
                .when(vulnerabilityScanService)
                .scanUnscannedReleasesOnly();

        systemDataInitializer.triggerRefresh();

        reset(vulnerabilityScanService);
        systemDataInitializer.triggerRefresh();
        verify(vulnerabilityScanService).scanUnscannedReleasesOnly();
    }

    @Test
    public void triggerRefresh_whenLockHeldLongerThanStaleThreshold_reclaimsLockAndRuns() {
        setJobRunning(true);
        systemDataInitializer.jobStartedAt.set(
                Instant.now().minus(SystemDataInitializer.STALE_JOB_THRESHOLD).minus(Duration.ofMinutes(1)));

        systemDataInitializer.triggerRefresh();

        verify(vulnerabilityScanService).scanUnscannedReleasesOnly();
        assertFalse(systemDataInitializer.pendingRefresh.get());
    }

    @Test
    public void triggerRefresh_whenLockHeldWithinStaleThreshold_staysQueued() {
        setJobRunning(true);
        systemDataInitializer.jobStartedAt.set(
                Instant.now().minus(SystemDataInitializer.STALE_JOB_THRESHOLD).plus(Duration.ofMinutes(1)));

        systemDataInitializer.triggerRefresh();

        verify(vulnerabilityScanService, never()).scanUnscannedReleasesOnly();
        assertTrue(systemDataInitializer.pendingRefresh.get());
    }

    @Test
    public void triggerRefresh_whenJobAlreadyRunning_setsPendingRefreshFlag() {
        setJobRunning(true);

        systemDataInitializer.triggerRefresh();

        assertTrue(systemDataInitializer.pendingRefresh.get());
    }

    @Test
    public void triggerRefresh_whenCompleted_leavesPendingRefreshFalse() {
        systemDataInitializer.triggerRefresh();

        assertFalse(systemDataInitializer.pendingRefresh.get());
    }

    @Test
    public void run_withoutArguments_importsAllDataAndScansRelevantReleases() throws Exception {
        runWithArgs();

        verify(gitHubRepositoryStatisticsService).fetchRepositoryStatistics();
        verify(issueProjectItemsInjectionService).injectIssueProjectItems();
        verify(releaseInjectionService).injectReleases();
        verify(releaseArtifactService).deleteObsoleteReleaseArtifacts();
        verify(vulnerabilityScanService).scanRelevantReleasesOnly();
        verify(vulnerabilityScanService, never()).scanUnscannedReleasesOnly();
        assertJobLockReleased();
    }

    @Test
    public void run_withImportArgument_importsAllDataWithoutScanningVulnerabilities() throws Exception {
        runWithArgs("--import");

        verify(issueProjectItemsInjectionService).injectIssueProjectItems();
        verify(releaseInjectionService).injectReleases();
        verify(vulnerabilityScanService, never()).scanUnscannedReleasesOnly();
        verify(vulnerabilityScanService, never()).scanRelevantReleasesOnly();
        assertJobLockReleased();
    }

    @Test
    public void run_withTrivyArgument_scansRelevantReleasesWithoutImportingData() throws Exception {
        runWithArgs("--trivy");

        verifyNoInteractions(gitHubRepositoryStatisticsService);
        verifyNoInteractions(
                labelInjectionService,
                milestoneInjectionService,
                issueTypeInjectionService,
                issueProjectItemsInjectionService,
                branchInjectionService,
                issueInjectionService,
                pullRequestInjectionService,
                releaseInjectionService,
                releaseArtifactService);
        verify(vulnerabilityScanService).scanRelevantReleasesOnly();
        verify(vulnerabilityScanService, never()).scanUnscannedReleasesOnly();
        assertJobLockReleased();
    }

    @Test
    public void run_withUnknownArgument_doesNoWork() throws Exception {
        runWithArgs("--unknown");

        verifyNoInteractions(
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
                vulnerabilityScanService);
        assertJobLockReleased();
    }

    @Test
    public void run_whenFetchDisabled_skipsImportsButStillScansRelevantReleases() throws Exception {
        systemDataInitializer.dataFetchEnabled = false;

        runWithArgs();

        verifyNoInteractions(
                gitHubRepositoryStatisticsService,
                labelInjectionService,
                milestoneInjectionService,
                issueTypeInjectionService,
                issueProjectItemsInjectionService,
                branchInjectionService,
                issueInjectionService,
                pullRequestInjectionService,
                releaseInjectionService,
                releaseArtifactService);
        verify(vulnerabilityScanService).scanRelevantReleasesOnly();
        verify(vulnerabilityScanService, never()).scanUnscannedReleasesOnly();
        assertJobLockReleased();
    }

    @Test
    public void run_whenApplicationExitFails_stillReleasesJobLock() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            springApplication
                    .when(() -> SpringApplication.exit(eq(appContext), any(ExitCodeGenerator[].class)))
                    .thenThrow(new IllegalStateException("Application exit failed"));

            assertThrows(IllegalStateException.class, () -> systemDataInitializer.run("--trivy"));

            assertJobLockReleased();
        }
    }

    @Test
    public void run_whenWebhookRefreshQueued_drainsItAfterStartupJobCompletes() throws Exception {
        AtomicBoolean refreshRequested = new AtomicBoolean();
        doAnswer(invocation -> {
                    if (refreshRequested.compareAndSet(false, true)) {
                        systemDataInitializer.triggerRefresh();
                    }
                    return null;
                })
                .when(gitHubRepositoryStatisticsService)
                .fetchRepositoryStatistics();

        runWithArgs();

        assertFalse(systemDataInitializer.pendingRefresh.get());
        assertJobLockReleased();
        verify(issueProjectItemsInjectionService, times(2)).injectIssueProjectItems();
        verify(vulnerabilityScanService).scanRelevantReleasesOnly();
        verify(vulnerabilityScanService).scanUnscannedReleasesOnly();
    }

    @Test
    public void run_whenRefreshIsQueuedDuringDrain_processesItAfterCurrentRefresh() throws Exception {
        AtomicInteger statisticsFetches = new AtomicInteger();
        doAnswer(invocation -> {
                    if (statisticsFetches.incrementAndGet() <= 2) {
                        systemDataInitializer.triggerRefresh();
                    }
                    return null;
                })
                .when(gitHubRepositoryStatisticsService)
                .fetchRepositoryStatistics();

        runWithArgs();

        assertFalse(systemDataInitializer.pendingRefresh.get());
        assertJobLockReleased();
        verify(releaseInjectionService, times(3)).injectReleases();
        verify(vulnerabilityScanService).scanRelevantReleasesOnly();
        verify(vulnerabilityScanService, times(2)).scanUnscannedReleasesOnly();
    }

    @Test
    public void fetchGitHubStatistics_whenFetchEnabled_fetchesStatistics() throws GitHubGraphQLClientException {
        systemDataInitializer.fetchGitHubStatistics();

        verify(gitHubRepositoryStatisticsService).fetchRepositoryStatistics();
    }

    @Test
    public void fetchGitHubStatistics_whenFetchDisabled_skipsStatistics() {
        systemDataInitializer.dataFetchEnabled = false;

        systemDataInitializer.fetchGitHubStatistics();

        verifyNoInteractions(gitHubRepositoryStatisticsService);
    }

    @Test
    public void fetchGitHubStatistics_whenClientFails_doesNotPropagateException() throws Exception {
        doThrow(new GitHubGraphQLClientException("GitHub unavailable", null))
                .when(gitHubRepositoryStatisticsService)
                .fetchRepositoryStatistics();

        assertDoesNotThrow(systemDataInitializer::fetchGitHubStatistics);
        verify(gitHubRepositoryStatisticsService).fetchRepositoryStatistics();
    }

    @Test
    public void run_whenJobAlreadyRunning_skipsAllWork() {
        setJobRunning(true);

        systemDataInitializer.run();

        verifyNoInteractions(
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
                vulnerabilityScanService);
    }

    @Test
    public void triggerRefresh_whenDataFetchDisabled_skipsAllWork() {
        systemDataInitializer.dataFetchEnabled = false;

        systemDataInitializer.triggerRefresh();

        verifyNoInteractions(releaseInjectionService, labelInjectionService, vulnerabilityScanService);
    }

    private void setJobRunning(boolean value) {
        systemDataInitializer.isJobRunning.set(value);
    }

    private void runWithArgs(String... args) {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            springApplication
                    .when(() -> SpringApplication.exit(eq(appContext), any(ExitCodeGenerator[].class)))
                    .thenReturn(0);
            systemDataInitializer.run(args);
            springApplication.verify(() -> SpringApplication.exit(eq(appContext), any(ExitCodeGenerator[].class)));
        }
    }

    private void assertJobLockReleased() {
        assertFalse(systemDataInitializer.isJobRunning.get());
        assertNull(systemDataInitializer.jobStartedAt.get());
    }
}
