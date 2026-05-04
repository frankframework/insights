package org.frankframework.insights.common.configuration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.github.graphql.GitHubRepositoryStatisticsDTO;
import org.frankframework.insights.github.graphql.GitHubRepositoryStatisticsService;
import org.frankframework.insights.github.graphql.GitHubTotalCountDTO;
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
        when(releaseService.getStoredReleaseCount()).thenReturn(0L);
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(null);

        systemDataInitializer.triggerRefresh();

        verify(vulnerabilityService).scanUnscannedReleasesOnly();
    }

    @Test
    public void triggerRefresh_whenJobAlreadyRunning_queuesRefreshWithoutRunningImmediately() {
        setJobRunning(true);

        systemDataInitializer.triggerRefresh();

        verify(releaseService, never()).getStoredReleaseCount();
        verify(vulnerabilityService, never()).scanUnscannedReleasesOnly();
    }

    @Test
    public void triggerRefresh_whenGitHubAndDbCountMatch_skipsFullInject() throws Exception {
        when(releaseService.getStoredReleaseCount()).thenReturn(5L);
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statsWithReleaseCount(5));

        systemDataInitializer.triggerRefresh();

        verify(labelService, never()).injectLabels();
        verify(milestoneService, never()).injectMilestones();
        verify(releaseService, never()).injectReleases();
    }

    @Test
    public void triggerRefresh_whenGitHubCountHigherThanDb_runsFullInject() throws Exception {
        when(releaseService.getStoredReleaseCount()).thenReturn(4L);
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statsWithReleaseCount(5));

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
    public void triggerRefresh_whenStatsDtoIsNull_treatsAsNewReleasesAndRunsFullInject() throws Exception {
        when(releaseService.getStoredReleaseCount()).thenReturn(5L);
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(null);

        systemDataInitializer.triggerRefresh();

        verify(labelService).injectLabels();
    }

    @Test
    public void triggerRefresh_whenReleasesFieldInDtoIsNull_treatsAsNewReleasesAndRunsFullInject() throws Exception {
        when(releaseService.getStoredReleaseCount()).thenReturn(5L);
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(new GitHubRepositoryStatisticsDTO(null, null, null, null));

        systemDataInitializer.triggerRefresh();

        verify(labelService).injectLabels();
    }

    @Test
    public void triggerRefresh_alwaysScansUnscannedReleases_evenWhenInjectSkipped() {
        when(releaseService.getStoredReleaseCount()).thenReturn(3L);
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statsWithReleaseCount(3));

        systemDataInitializer.triggerRefresh();

        verify(vulnerabilityService).scanUnscannedReleasesOnly();
        verify(vulnerabilityService, never()).scanAndSaveVulnerabilitiesForAllReleases();
    }

    @Test
    public void triggerRefresh_alwaysScansUnscannedReleases_afterFullInject() {
        when(releaseService.getStoredReleaseCount()).thenReturn(4L);
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statsWithReleaseCount(5));

        systemDataInitializer.triggerRefresh();

        verify(vulnerabilityService).scanUnscannedReleasesOnly();
        verify(vulnerabilityService, never()).scanAndSaveVulnerabilitiesForAllReleases();
    }

    @Test
    public void triggerRefresh_whenInjectThrows_logsErrorAndResetsLock() throws Exception {
        when(releaseService.getStoredReleaseCount()).thenReturn(4L);
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statsWithReleaseCount(5));
        doThrow(new RuntimeException("GitHub unreachable")).when(labelService).injectLabels();

        systemDataInitializer.triggerRefresh();

        reset(labelService);
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(null);
        systemDataInitializer.triggerRefresh();
        verify(vulnerabilityService, times(2)).scanUnscannedReleasesOnly();
    }

    @Test
    public void triggerRefresh_whenScanThrows_logsErrorAndResetsLock() {
        when(releaseService.getStoredReleaseCount()).thenReturn(3L);
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statsWithReleaseCount(3));
        doThrow(new RuntimeException("Trivy unavailable"))
                .when(vulnerabilityService)
                .scanUnscannedReleasesOnly();

        systemDataInitializer.triggerRefresh();

        reset(vulnerabilityService);
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(null);
        systemDataInitializer.triggerRefresh();
        verify(vulnerabilityService).scanUnscannedReleasesOnly();
    }

    @Test
    public void triggerRefresh_whenJobAlreadyRunning_setsPendingRefreshFlag() {
        setJobRunning(true);

        systemDataInitializer.triggerRefresh();

        assertTrue(systemDataInitializer.pendingRefresh.get());
    }

    @Test
    public void triggerRefresh_whenCompleted_leavesPendingRefreshFalse() {
        when(releaseService.getStoredReleaseCount()).thenReturn(3L);
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statsWithReleaseCount(3));

        systemDataInitializer.triggerRefresh();

        assertFalse(systemDataInitializer.pendingRefresh.get());
    }

    @Test
    public void run_whenPendingRefreshQueued_drainsPendingRefreshAfterCompletion() {
        systemDataInitializer.pendingRefresh.set(true);
        when(releaseService.getStoredReleaseCount()).thenReturn(3L);
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statsWithReleaseCount(3));

        systemDataInitializer.run();

        assertFalse(systemDataInitializer.pendingRefresh.get());
    }

    @Test
    public void dailyJob_whenPendingRefreshQueued_drainsPendingRefreshAfterCompletion() {
        systemDataInitializer.pendingRefresh.set(true);
        when(releaseService.getStoredReleaseCount()).thenReturn(3L);
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statsWithReleaseCount(3));

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

    private static GitHubRepositoryStatisticsDTO statsWithReleaseCount(int count) {
        return new GitHubRepositoryStatisticsDTO(null, null, null, new GitHubTotalCountDTO(count));
    }

    private void setJobRunning(boolean value) {
        systemDataInitializer.isJobRunning.set(value);
    }
}
