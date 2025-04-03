package org.frankframework.insights.pullrequest;

import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.commit.Commit;
import org.frankframework.insights.common.configuration.GitHubProperties;
import org.frankframework.insights.common.entityconnection.branchcommit.BranchCommit;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequest;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequestRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PullRequestService {
	private final GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;
	private final GitHubClient gitHubClient;
	private final Mapper mapper;
	private final BranchPullRequestRepository branchPullRequestRepository;
	private final BranchService branchService;
	private final List<String> branchProtectionRegexes;

	public PullRequestService(
			GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
			GitHubClient gitHubClient,
			Mapper mapper,
			BranchPullRequestRepository branchPullRequestRepository,
			BranchService branchService,
			GitHubProperties gitHubProperties) {
		this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
		this.gitHubClient = gitHubClient;
		this.mapper = mapper;
		this.branchPullRequestRepository = branchPullRequestRepository;
		this.branchService = branchService;
		this.branchProtectionRegexes = gitHubProperties.getBranchProtectionRegexes();
	}

	public void injectPullRequests() {
		Map<String, Integer> githubPullRequestCounts = fetchGitHubPullRequestCounts();

		List<Branch> branches = branchService.getAllBranches();
		log.info("Fetched {} branches", branches.size());

		List<Branch> branchesToUpdate = filterBranchesToUpdate(branches, githubPullRequestCounts);
		log.info("Found {} branches to update", branchesToUpdate.size());

		if (branchesToUpdate.isEmpty()) {
			log.info("No branches to update pull requests of. Skipping...");
			return;
		}

		updateBranches(branchesToUpdate);
	}

	private Map<String, Integer> fetchGitHubPullRequestCounts() {
		return gitHubRepositoryStatisticsService
				.getGitHubRepositoryStatisticsDTO()
				.getGitHubPullRequestsCounts(branchProtectionRegexes);
	}

	private List<Branch> filterBranchesToUpdate(List<Branch> branches, Map<String, Integer> githubPullRequestCounts) {
		return branches.stream()
				.filter(branch -> {
					int databasePullRequestCount = branchPullRequestRepository.countBranchPullRequestByBranch(branch);
					int githubPullRequestCount = githubPullRequestCounts.getOrDefault(branch.getName(), 0);

					log.info("{} pull requests found in database, {} pull requests found in GitHub, for branch {}",
							databasePullRequestCount, githubPullRequestCount, branch.getName());

					return databasePullRequestCount != githubPullRequestCount;
				})
				.toList();
	}

	private void updateBranches(List<Branch> branchesToUpdate) {
		Set<Branch> updatedBranches = branchesToUpdate.stream()
				.map(branch -> {
					try {
						return getPullRequestsForBranch(branch);
					} catch (PullRequestInjectionException e) {
						log.error("Failed to update pull requests for branch: {}", branch.getName(), e);
						return null;
					}
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		branchService.saveBranches(updatedBranches);
	}

	private Branch getPullRequestsForBranch(Branch branch) throws PullRequestInjectionException {
		try {
			Set<PullRequestDTO> pullRequestDTOs = gitHubClient.getBranchPullRequests(branch.getName());
			Set<PullRequest> pullRequests = mapper.toEntity(pullRequestDTOs, PullRequest.class);

			Set<BranchPullRequest> branchPullRequests = new HashSet<>(getBranchPullRequestsForBranch(branch));

			Set<BranchPullRequest> updatedBranchPullRequests = processBranchPullRequests(branch, pullRequests, branchPullRequests);

			branchPullRequests.clear();
			branchPullRequests.addAll(updatedBranchPullRequests);
			branch.setBranchPullRequests(branchPullRequests);

			log.info("Updated branch {} with {} pull requests", branch.getName(), updatedBranchPullRequests.size());
			return branch;
		} catch (Exception e) {
			throw new PullRequestInjectionException("Error while injecting GitHub pull requests and setting them to branches", e);
		}
	}

	private Set<BranchPullRequest> getBranchPullRequestsForBranch(Branch branch) {
		return branchPullRequestRepository.findBranchPullRequestByBranchId(branch.getId());
	}

	private Set<BranchPullRequest> processBranchPullRequests(Branch branch, Set<PullRequest> pullRequests, Set<BranchPullRequest> branchPullRequests) {
		Map<String, BranchPullRequest> existingPullRequestsMap = branchPullRequests.stream()
				.collect(Collectors.toMap(bpr -> bpr.getPullRequest().getId(), bpr -> bpr));

		return pullRequests.stream()
				.map(pullRequest -> getOrCreateBranchPullRequest(branch, pullRequest, existingPullRequestsMap))
				.collect(Collectors.toSet());
	}

	private BranchPullRequest getOrCreateBranchPullRequest(Branch branch, PullRequest pullRequest, Map<String, BranchPullRequest> existingPullRequestsMap) {
		return existingPullRequestsMap.getOrDefault(pullRequest.getId(), new BranchPullRequest(branch, pullRequest));
	}
}
