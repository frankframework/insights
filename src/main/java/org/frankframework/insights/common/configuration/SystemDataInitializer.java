package org.frankframework.insights.common.configuration;

import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.github.graphql.GitHubGraphQLClientException;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@Slf4j
public class SystemDataInitializer implements CommandLineRunner {
    private final AtomicBoolean isJobRunning = new AtomicBoolean(false);

    private final GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;
    private final LabelService labelService;
    private final MilestoneService milestoneService;
    private final IssueTypeService issueTypeService;
    private final IssueProjectItemsService issueProjectItemsService;
    private final BranchService branchService;
    private final IssueService issueService;
    private final PullRequestService pullRequestService;
    private final ReleaseService releaseService;
    private final ReleaseArtifactService releaseArtifactService;
    private final VulnerabilityService vulnerabilityService;

    @Value("${data.fetch-enabled}")
    private boolean dataFetchEnabled;

    public SystemDataInitializer(
            GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
            LabelService labelService,
            MilestoneService milestoneService,
            IssueTypeService issueTypeService,
            IssueProjectItemsService issueProjectItemsService,
            BranchService branchService,
            IssueService issueService,
            PullRequestService pullRequestService,
            ReleaseService releaseService,
            ReleaseArtifactService releaseArtifactService,
            VulnerabilityService vulnerabilityService) {
        this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
        this.labelService = labelService;
        this.milestoneService = milestoneService;
        this.issueTypeService = issueTypeService;
        this.issueProjectItemsService = issueProjectItemsService;
        this.branchService = branchService;
        this.issueService = issueService;
        this.pullRequestService = pullRequestService;
        this.releaseService = releaseService;
        this.releaseArtifactService = releaseArtifactService;
        this.vulnerabilityService = vulnerabilityService;
    }

    /**
     * CommandLineRunner method that runs on application startup.
     * @param args command line arguments
     */
    @Override
    @SchedulerLock(name = "startUpGitHubUpdate", lockAtMostFor = "PT2H", lockAtLeastFor = "PT30M")
    public void run(String... args) {
        if (!isJobRunning.compareAndSet(false, true)) {
            log.warn("Startup job skipped: another job is already running");
            return;
        }
        try {
            log.info("Startup: Fetching GitHub statistics");
            fetchGitHubStatistics();
            log.info("Startup: Fetching full system data");
            initializeSystemData();
        } finally {
            isJobRunning.set(false);
        }
    }

    /**
     * Scheduled job that runs daily at midnight.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(name = "dailyGitHubUpdate", lockAtMostFor = "PT2H", lockAtLeastFor = "PT30M")
    public void dailyJob() {
        if (!isJobRunning.compareAndSet(false, true)) {
            log.warn("Daily job skipped: another job is already running");
            return;
        }
        try {
            log.info("Daily fetch job started");
            fetchGitHubStatistics();
            initializeSystemData();
        } finally {
            isJobRunning.set(false);
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

            log.info("Start fetching all GitHub data");
            labelService.injectLabels();
            milestoneService.injectMilestones();
            issueTypeService.injectIssueTypes();
            issueProjectItemsService.injectIssueProjectItems();
            branchService.injectBranches();
            issueService.injectIssues();
            pullRequestService.injectBranchPullRequests();
            releaseService.injectReleases();
            log.info("Done fetching all GitHub data");

            log.info("Cleaning up obsolete release artifacts");
            releaseArtifactService.deleteObsoleteReleaseArtifacts();

            log.info("Start fetching vulnerability data");
            vulnerabilityService.scanAndSaveVulnerabilitiesForAllReleases();

            log.info("Done fetching all vulnerability data");
        } catch (Exception e) {
            log.error("Error initializing system data", e);
        }
    }
}
