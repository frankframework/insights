package org.frankframework.insights.common.configuration;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.common.configuration.properties.FetchProperties;
import org.frankframework.insights.github.GitHubClientException;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.frankframework.insights.issue.IssueService;
import org.frankframework.insights.issuePriority.IssuePriorityService;
import org.frankframework.insights.issuetype.IssueTypeService;
import org.frankframework.insights.label.LabelService;
import org.frankframework.insights.milestone.MilestoneService;
import org.frankframework.insights.pullrequest.PullRequestService;
import org.frankframework.insights.release.ReleaseService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@Slf4j
public class GitHubConfiguration implements CommandLineRunner {
    private final GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;
    private final LabelService labelService;
    private final MilestoneService milestoneService;
    private final IssueTypeService issueTypeService;
    private final IssuePriorityService issuePriorityService;
    private final BranchService branchService;
    private final IssueService issueService;
    private final PullRequestService pullRequestService;
    private final ReleaseService releaseService;
    private final Boolean fetchEnabled;

    public GitHubConfiguration(
            GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
            LabelService labelService,
            MilestoneService milestoneService,
            IssueTypeService issueTypeService,
            IssuePriorityService issuePriorityService,
            BranchService branchService,
            IssueService issueService,
            PullRequestService pullRequestService,
            ReleaseService releaseService,
            FetchProperties fetchProperties) {
        this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
        this.labelService = labelService;
        this.milestoneService = milestoneService;
        this.issueTypeService = issueTypeService;
        this.issuePriorityService = issuePriorityService;
        this.branchService = branchService;
        this.issueService = issueService;
        this.pullRequestService = pullRequestService;
        this.releaseService = releaseService;
        this.fetchEnabled = fetchProperties.getEnabled();
    }

    /**
     * CommandLineRunner method that runs on application startup.
     * @param args command line arguments
     */
    @Override
    @SchedulerLock(name = "startUpGitHubUpdate", lockAtMostFor = "PT2H", lockAtLeastFor = "PT30M")
    public void run(String... args) {
        log.info("Startup: Fetching GitHub statistics");
        fetchGitHubStatistics();
        log.info("Startup: Fetching all GitHub data");
        initializeGitHubData();
    }

    /**
     * Scheduled job that runs daily at midnight.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(name = "dailyGitHubUpdate", lockAtMostFor = "PT2H", lockAtLeastFor = "PT30M")
    public void dailyJob() {
        log.info("Daily fetch job started");
        fetchGitHubStatistics();
        initializeGitHubData();
    }

    /**
     * Fetches GitHub statistics and updates the database.
     */
    @SchedulerLock(name = "fetchGitHubStatistics", lockAtMostFor = "PT10M")
    public void fetchGitHubStatistics() {
        try {
            if (!fetchEnabled) {
                log.info("Skipping GitHub fetch: skipping due to build/test configuration.");
                return;
            }

            gitHubRepositoryStatisticsService.fetchRepositoryStatistics();
        } catch (GitHubClientException e) {
            log.error("Error fetching GitHub statistics", e);
        }
    }

    /**
     * Initializes data by fetching labels, milestones, branches, issues, pull requests, and releases from GitHub.
     */
    @SchedulerLock(name = "initializeGitHubData", lockAtMostFor = "PT2H")
    public void initializeGitHubData() {
        try {
            if (!fetchEnabled) {
                log.info("Skipping GitHub fetch: skipping due to build/test configuration.");
                return;
            }

            log.info("Start fetching all GitHub data");
            labelService.injectLabels();
            milestoneService.injectMilestones();
            issueTypeService.injectIssueTypes();
            issuePriorityService.injectIssuePriorities();
            branchService.injectBranches();
            issueService.injectIssues();
            pullRequestService.injectBranchPullRequests();
            releaseService.injectReleases();

            log.info("Done fetching all GitHub data");
        } catch (Exception e) {
            log.error("Error initializing GitHub data", e);
        }
    }
}
