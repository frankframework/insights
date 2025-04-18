package org.frankframework.insights.common.configuration;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.commit.CommitService;
import org.frankframework.insights.github.GitHubClientException;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.frankframework.insights.issue.IssueService;
import org.frankframework.insights.label.LabelService;
import org.frankframework.insights.milestone.MilestoneService;
import org.frankframework.insights.pullrequest.PullRequestService;
import org.frankframework.insights.release.ReleaseService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@Slf4j
public class SystemDataInitializer implements CommandLineRunner {

    private final GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;
    private final LabelService labelService;
    private final MilestoneService milestoneService;
    private final BranchService branchService;
    private final CommitService commitService;
    private final IssueService issueService;
    private final PullRequestService pullRequestService;
    private final ReleaseService releaseService;

    public SystemDataInitializer(
            GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
            LabelService labelService,
            MilestoneService milestoneService,
            BranchService branchService,
            CommitService commitService,
            IssueService issueService,
            PullRequestService pullRequestService,
            ReleaseService releaseService) {
        this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
        this.labelService = labelService;
        this.milestoneService = milestoneService;
        this.branchService = branchService;
        this.commitService = commitService;
        this.issueService = issueService;
        this.pullRequestService = pullRequestService;
        this.releaseService = releaseService;
    }

    @Override
    @SchedulerLock(name = "startUpGitHubUpdate", lockAtMostFor = "PT2H", lockAtLeastFor = "PT30M")
    public void run(String... args) {
        log.info("Startup: Fetching GitHub statistics");
        fetchGitHubStatistics();
        log.info("Startup: Fetching full system data");
        initializeSystemData();
    }

	@Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(name = "dailyGitHubUpdate", lockAtMostFor = "PT2H", lockAtLeastFor = "PT30M")
    public void dailyJob() {
        log.info("Daily fetch job started");
        fetchGitHubStatistics();
        initializeSystemData();
    }

    @SchedulerLock(name = "fetchGitHubStatistics", lockAtMostFor = "PT10M")
    public void fetchGitHubStatistics() {
        try {
			String gitHubFetchEnabledProperty = System.getProperty("githubFetchEnabled", "true");
			if (!Boolean.parseBoolean(gitHubFetchEnabledProperty)) {
				log.info("Skipping GitHub fetch: skipping due to build/test configuration.");
				return;
			}

            gitHubRepositoryStatisticsService.fetchRepositoryStatistics();
        } catch (GitHubClientException e) {
            log.error("Error fetching GitHub statistics", e);
        }
    }

    @SchedulerLock(name = "initializeSystemData", lockAtMostFor = "PT2H")
    public void initializeSystemData() {
        try {
			String gitHubFetchEnabledProperty = System.getProperty("githubFetchEnabled", "true");
			if (!Boolean.parseBoolean(gitHubFetchEnabledProperty)) {
				log.info("Skipping GitHub fetch: skipping due to build/test configuration.");
				return;
			}

            log.info("Start fetching all GitHub data");
            labelService.injectLabels();
            milestoneService.injectMilestones();
            branchService.injectBranches();
            commitService.injectBranchCommits();
            issueService.injectIssues();
            pullRequestService.injectBranchPullRequests();
            releaseService.injectReleases();

            log.info("Done fetching all GitHub data");
        } catch (Exception e) {
            log.error("Error initializing system data", e);
        }
    }
}
