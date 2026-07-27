package org.frankframework.insights.common.configuration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import org.frankframework.insights.branch.BranchService;
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
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@ExtendWith(MockitoExtension.class)
public class SystemDataInitializerTest {

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

    private final TaskExecutor taskExecutor = new SyncTaskExecutor();

    private SystemDataInitializer systemDataInitializer;

    @BeforeEach
    public void setUp() {
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
                vulnerabilityService,
                taskExecutor);

        systemDataInitializer.dataFetchEnabled = true;
    }

    @Test
    public void triggerRefresh_whenNoJobRunning_startsWork() {
        systemDataInitializer.triggerRefresh();

        verify(vulnerabilityService).scanUnscannedReleasesOnly();
    }

    @Test
    public void triggerRefresh_whenJobAlreadyRunning_queuesRefreshWithoutRunningImmediately() throws Exception {
        setJobRunning(true);

        systemDataInitializer.triggerRefresh();

        verify(releaseService, never()).injectReleases();
        verify(vulnerabilityService, never()).scanUnscannedReleasesOnly();
    }

    @Test
    public void triggerRefresh_alwaysRunsFullInject() throws Exception {
        systemDataInitializer.triggerRefresh();

        verify(labelService).injectLabels();
        verify(milestoneService).injectMilestones();
        verify(issueTypeService).injectIssueTypes();
        verify(branchService).injectBranches();
        verify(issueService).injectIssues();
        verify(pullRequestService).injectBranchPullRequests();
        verify(releaseService).injectReleases();
        verify(releaseArtifactService).deleteObsoleteReleaseArtifacts();
    }

    @Test
    public void triggerRefresh_alwaysScansUnscannedReleasesOnly_notFullRescan() {
        systemDataInitializer.triggerRefresh();

        verify(vulnerabilityService).scanUnscannedReleasesOnly();
        verify(vulnerabilityService, never()).scanAndSaveVulnerabilitiesForAllReleases();
    }

    @Test
    public void triggerRefresh_whenInjectThrows_logsErrorAndResetsLock() throws Exception {
        doThrow(new RuntimeException("GitHub unreachable")).when(labelService).injectLabels();

        systemDataInitializer.triggerRefresh();

        reset(labelService);
        systemDataInitializer.triggerRefresh();
        verify(vulnerabilityService, times(2)).scanUnscannedReleasesOnly();
    }

    @Test
    public void triggerRefresh_whenEarlierStepThrows_laterStepsIncludingReleaseInjectionStillRun() throws Exception {
        doThrow(new RuntimeException("GitHub unreachable")).when(labelService).injectLabels();

        systemDataInitializer.triggerRefresh();

        verify(milestoneService).injectMilestones();
        verify(branchService).injectBranches();
        verify(releaseService).injectReleases();
        verify(releaseArtifactService).deleteObsoleteReleaseArtifacts();
        verify(vulnerabilityService).scanUnscannedReleasesOnly();
    }

    @Test
    public void triggerRefresh_whenReleaseInjectionItselfThrows_laterStepsStillRunAndScanStillHappens()
            throws Exception {
        doThrow(new RuntimeException("GitHub unreachable")).when(releaseService).injectReleases();

        systemDataInitializer.triggerRefresh();

        verify(releaseArtifactService).deleteObsoleteReleaseArtifacts();
        verify(vulnerabilityService).scanUnscannedReleasesOnly();
    }

    @Test
    public void triggerRefresh_whenScanThrows_logsErrorAndResetsLock() {
        doThrow(new RuntimeException("Trivy unavailable"))
                .when(vulnerabilityService)
                .scanUnscannedReleasesOnly();

        systemDataInitializer.triggerRefresh();

        reset(vulnerabilityService);
        systemDataInitializer.triggerRefresh();
        verify(vulnerabilityService).scanUnscannedReleasesOnly();
    }

    @Test
    public void triggerRefresh_whenLockHeldLongerThanStaleThreshold_reclaimsLockAndRuns() {
        setJobRunning(true);
        systemDataInitializer.jobStartedAt.set(
                Instant.now().minus(SystemDataInitializer.STALE_JOB_THRESHOLD).minus(Duration.ofMinutes(1)));

        systemDataInitializer.triggerRefresh();

        verify(vulnerabilityService).scanUnscannedReleasesOnly();
        assertFalse(systemDataInitializer.pendingRefresh.get());
    }

    @Test
    public void triggerRefresh_whenLockHeldWithinStaleThreshold_staysQueued() {
        setJobRunning(true);
        systemDataInitializer.jobStartedAt.set(
                Instant.now().minus(SystemDataInitializer.STALE_JOB_THRESHOLD).plus(Duration.ofMinutes(1)));

        systemDataInitializer.triggerRefresh();

        verify(vulnerabilityService, never()).scanUnscannedReleasesOnly();
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
    public void run_whenPendingRefreshQueued_drainsPendingRefreshAfterCompletion() {
        systemDataInitializer.pendingRefresh.set(true);

        systemDataInitializer.run();

        assertFalse(systemDataInitializer.pendingRefresh.get());
    }

    @Test
    public void dailyJob_whenPendingRefreshQueued_drainsPendingRefreshAfterCompletion() {
        systemDataInitializer.pendingRefresh.set(true);

        systemDataInitializer.dailyJob();

        assertFalse(systemDataInitializer.pendingRefresh.get());
    }

    @Test
    public void run_whenJobAlreadyRunning_skipsAllWork() {
        setJobRunning(true);

        systemDataInitializer.run();

        verifyNoInteractions(gitHubRepositoryStatisticsService, labelService, vulnerabilityService);
    }

    @Test
    public void dailyJob_whenJobAlreadyRunning_skipsAllWork() {
        setJobRunning(true);

        systemDataInitializer.dailyJob();

        verifyNoInteractions(gitHubRepositoryStatisticsService, labelService, vulnerabilityService);
    }

    @Test
    public void triggerRefresh_whenDataFetchDisabled_skipsAllWork() {
        systemDataInitializer.dataFetchEnabled = false;

        systemDataInitializer.triggerRefresh();

        verifyNoInteractions(releaseService, labelService, vulnerabilityService);
    }

    private void setJobRunning(boolean value) {
        systemDataInitializer.isJobRunning.set(value);
    }
}
