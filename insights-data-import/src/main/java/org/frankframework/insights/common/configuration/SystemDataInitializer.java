package org.frankframework.insights.common.configuration;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@Slf4j
public class SystemDataInitializer implements CommandLineRunner {
    protected static final Duration STALE_JOB_THRESHOLD = Duration.ofHours(2);

    protected final AtomicBoolean isJobRunning = new AtomicBoolean(false);
    protected final AtomicReference<Instant> jobStartedAt = new AtomicReference<>();
    protected final AtomicBoolean pendingRefresh = new AtomicBoolean(false);

    private final GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;
    private final LabelInjectionService labelInjectionService;
    private final MilestoneInjectionService milestoneInjectionService;
    private final IssueTypeInjectionService issueTypeInjectionService;
    private final IssueProjectItemsInjectionService issueProjectItemsInjectionService;
    private final BranchInjectionService branchInjectionService;
    private final IssueInjectionService issueInjectionService;
    private final PullRequestInjectionService pullRequestInjectionService;
    private final ReleaseInjectionService releaseInjectionService;
    private final ReleaseArtifactService releaseArtifactService;
    private final VulnerabilityScanService vulnerabilityScanService;
    private final TaskExecutor taskExecutor;

    @Value("${data.fetch-enabled}")
    protected boolean dataFetchEnabled;

    public SystemDataInitializer(
            GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
            LabelInjectionService labelInjectionService,
            MilestoneInjectionService milestoneInjectionService,
            IssueTypeInjectionService issueTypeInjectionService,
            IssueProjectItemsInjectionService issueProjectItemsInjectionService,
            BranchInjectionService branchInjectionService,
            IssueInjectionService issueInjectionService,
            PullRequestInjectionService pullRequestInjectionService,
            ReleaseInjectionService releaseInjectionService,
            ReleaseArtifactService releaseArtifactService,
            VulnerabilityScanService vulnerabilityScanService,
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
        this.labelInjectionService = labelInjectionService;
        this.milestoneInjectionService = milestoneInjectionService;
        this.issueTypeInjectionService = issueTypeInjectionService;
        this.issueProjectItemsInjectionService = issueProjectItemsInjectionService;
        this.branchInjectionService = branchInjectionService;
        this.issueInjectionService = issueInjectionService;
        this.pullRequestInjectionService = pullRequestInjectionService;
        this.releaseInjectionService = releaseInjectionService;
        this.releaseArtifactService = releaseArtifactService;
        this.vulnerabilityScanService = vulnerabilityScanService;
        this.taskExecutor = taskExecutor;
    }

    /**
     * CommandLineRunner method that runs on application startup.
     * @param args command line arguments
     */
    @Override
    @SchedulerLock(name = "startUpGitHubUpdate", lockAtMostFor = "PT2H", lockAtLeastFor = "PT30M")
    public void run(String... args) {
        if (!tryAcquireJobLock()) {
            log.warn("Startup job skipped: another job is already running");
            return;
        }
        try {
            log.info("Startup: Fetching GitHub statistics");
            fetchGitHubStatistics();
            log.info("Startup: Fetching full system data");
            initializeSystemData();
        } finally {
            releaseJobLock();
            drainPendingRefresh();
        }
    }

    /**
     * Scheduled job that runs daily at midnight.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(name = "dailyGitHubUpdate", lockAtMostFor = "PT2H", lockAtLeastFor = "PT30M")
    public void dailyJob() {
        if (!tryAcquireJobLock()) {
            log.warn("Daily job skipped: another job is already running");
            return;
        }
        try {
            log.info("Daily fetch job started");
            fetchGitHubStatistics();
            initializeSystemData();
        } finally {
            releaseJobLock();
            drainPendingRefresh();
        }
    }

    /**
     * Schedules a data refresh triggered by a GitHub release webhook.
     * If a job is already running the refresh is queued and will execute immediately after.
     */
    public void triggerRefresh() {
        taskExecutor.execute(() -> {
            if (!tryAcquireJobLock()) {
                log.info("Refresh requested but a job is already running; queuing for after current job");
                pendingRefresh.set(true);
                return;
            }
            try {
                doWebhookRefresh("webhook trigger");
            } finally {
                releaseJobLock();
                drainPendingRefresh();
            }
        });
    }

    private synchronized boolean tryAcquireJobLock() {
        if (isJobRunning.compareAndSet(false, true)) {
            jobStartedAt.set(Instant.now());
            return true;
        }

        Instant startedAt = jobStartedAt.get();
        if (startedAt != null && Duration.between(startedAt, Instant.now()).compareTo(STALE_JOB_THRESHOLD) > 0) {
            log.error(
                    "Job lock has been held since {} (over {}); assuming the previous run hung and reclaiming it.",
                    startedAt,
                    STALE_JOB_THRESHOLD);
            isJobRunning.set(true);
            jobStartedAt.set(Instant.now());
            return true;
        }

        return false;
    }

    private synchronized void releaseJobLock() {
        isJobRunning.set(false);
        jobStartedAt.set(null);
    }

    /**
     * Runs the webhook-triggered refresh: injects all GitHub data, then scans any unscanned releases.
     */
    private void doWebhookRefresh(String context) {
        if (!dataFetchEnabled) {
            log.info("Skipping webhook refresh: data fetch is disabled.");
            return;
        }
        log.info("Webhook-triggered data refresh started ({})", context);

        scanForDataInjection(context);
        scanVulnerabilitiesForNewReleases();

        log.info("Webhook-triggered data refresh completed ({})", context);
    }

    private void scanForDataInjection(String context) {
        try {
            fetchGitHubStatistics();
            injectAllGitHubData();
        } catch (Exception e) {
            log.error("Webhook-triggered data inject failed ({})", context, e);
        }
    }

    private void scanVulnerabilitiesForNewReleases() {
        try {
            vulnerabilityScanService.scanUnscannedReleasesOnly();
        } catch (Exception e) {
            log.error("Error scanning vulnerabilities for new releases", e);
        }
    }

    /**
     * If a webhook refresh was queued while a scheduled job was running, execute it now.
     */
    private void drainPendingRefresh() {
        while (pendingRefresh.compareAndSet(true, false)) {
            if (!tryAcquireJobLock()) {
                pendingRefresh.set(true);
                return;
            }
            try {
                doWebhookRefresh("queued after scheduled job");
            } finally {
                releaseJobLock();
            }
        }
    }

    /**
     * Fetches GitHub statistics and updates the database.
     */
    @SchedulerLock(name = "fetchGitHubStatistics", lockAtMostFor = "PT10M")
    public void fetchGitHubStatistics() {
        try {
            if (!dataFetchEnabled) {
                log.info("Skipping data fetch: skipping due to build/test configuration.");
                return;
            }

            gitHubRepositoryStatisticsService.fetchRepositoryStatistics();
        } catch (GitHubGraphQLClientException e) {
            log.error("Error fetching data statistics", e);
        }
    }

    /**
     * Initializes system data by fetching labels, milestones, branches, issues, pull requests, releases, dependencies and vulnerabilities.
     */
    @SchedulerLock(name = "initializeSystemData", lockAtMostFor = "PT2H")
    public void initializeSystemData() {
        try {
            if (!dataFetchEnabled) {
                log.info("Skipping data fetch: skipping due to build/test configuration.");
                return;
            }

            injectAllGitHubData();

            log.info("Start fetching vulnerability data for all releases");
            vulnerabilityScanService.scanAndSaveVulnerabilitiesForAllReleases();
            log.info("Done fetching all vulnerability data");
        } catch (Exception e) {
            log.error("Error initializing system data", e);
        }
    }

    /**
     * Injects all GitHub data into the database. Shared by the daily job and the API-triggered refresh.
     * Does not scan vulnerabilities.
     */
    private void injectAllGitHubData() {
        log.info("Start injecting all GitHub data");
        runInjectionStep("labels", labelInjectionService::injectLabels);
        runInjectionStep("milestones", milestoneInjectionService::injectMilestones);
        runInjectionStep("issueTypes", issueTypeInjectionService::injectIssueTypes);
        runInjectionStep("issueProjectItems", issueProjectItemsInjectionService::injectIssueProjectItems);
        runInjectionStep("branches", branchInjectionService::injectBranches);
        runInjectionStep("issues", issueInjectionService::injectIssues);
        runInjectionStep("branchPullRequests", pullRequestInjectionService::injectBranchPullRequests);
        runInjectionStep("releases", releaseInjectionService::injectReleases);
        runInjectionStep("obsoleteReleaseArtifacts", releaseArtifactService::deleteObsoleteReleaseArtifacts);
        log.info("Done injecting all GitHub data");
    }

    private void runInjectionStep(String stepName, InjectionStep step) {
        try {
            step.run();
        } catch (Exception exception) {
            log.error("GitHub data injection step '{}' failed; continuing with remaining steps", stepName, exception);
        }
    }

    @FunctionalInterface
    private interface InjectionStep {
        void run() throws Exception;
    }
}
