package org.frankframework.insights.release;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.commit.Commit;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequest;
import org.frankframework.insights.pullrequest.PullRequest;
import org.frankframework.insights.common.entityconnection.ReleaseCommit;
import org.frankframework.insights.common.entityconnection.ReleasePullRequest;
import org.frankframework.insights.common.entityconnection.branchcommit.BranchCommit;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReleaseService {

	private final GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;
	private final GitHubClient gitHubClient;
	private final Mapper mapper;
	private final ReleaseRepository releaseRepository;
	private final BranchService branchService;

	public ReleaseService(
			GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
			GitHubClient gitHubClient,
			Mapper mapper,
			ReleaseRepository releaseRepository,
			BranchService branchService) {
		this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
		this.gitHubClient = gitHubClient;
		this.mapper = mapper;
		this.releaseRepository = releaseRepository;
		this.branchService = branchService;
	}

	public void injectReleases() throws ReleaseInjectionException {
		if (gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubReleaseCount()
				== releaseRepository.count()) {
			log.info("Releases already exist in the database.");
			return;
		}

		try {
			log.info("Fetching GitHub releases...");
			Set<ReleaseDTO> releaseDTOs = gitHubClient.getReleases();
			List<Branch> branchesWithCommits = branchService.getBranchesWithCommits();  // Fetch branches with commits
			List<Branch> branchesWithPullRequests = branchService.getBranchesWithPullRequests(branchesWithCommits);  // Fetch branches with pull requests

			List<Release> releasesWithBranches = releaseDTOs.stream()
					.map(dto -> createReleaseWithBranch(dto, branchesWithCommits))
					.filter(Objects::nonNull)
					.collect(Collectors.toList());

			if (releasesWithBranches.isEmpty()) {
				log.info("No releases with detected base branch to inject.");
				return;
			}

			Set<Release> releasesWithNewCommitsAndPRs = assignNewCommitsAndPRsToReleases(releasesWithBranches, branchesWithPullRequests);

			saveAllReleases(releasesWithNewCommitsAndPRs);
		} catch (Exception e) {
			throw new ReleaseInjectionException("Error injecting GitHub releases.", e);
		}
	}

	private Release createReleaseWithBranch(ReleaseDTO dto, List<Branch> branches) {
		if (dto.getTagCommit() == null || dto.getTagCommit().getCommitSha() == null || branches.isEmpty()) {
			log.warn("Skipping release '{}' due to missing info.", dto.getTagName());
			return null;
		}

		Release release = mapper.toEntity(dto, Release.class);
		release.setBranch(findBranchForRelease(release, branches));

		if (release.getBranch() == null) {
			log.warn("No matching branch found for release '{}'.", dto.getTagName());
			return null;
		}

		return release;
	}

	private Branch findBranchForRelease(Release release, List<Branch> branches) {
		return branches.stream()
				.filter(branch -> branchService.doesBranchContainCommit(branch, release.getCommitSha()))
				.max(Comparator.comparing(branch -> "master".equals(branch.getName()) ? 1 : 0))
				.orElse(null);
	}

	private Set<Release> assignNewCommitsAndPRsToReleases(List<Release> releases, List<Branch> branchesWithPullRequests) {
		Map<Branch, List<Release>> releasesByBranch = releases.stream()
				.filter(release -> release.getBranch() != null)
				.collect(Collectors.groupingBy(Release::getBranch));

		Set<Release> updatedReleases = new HashSet<>();

		for (Map.Entry<Branch, List<Release>> entry : releasesByBranch.entrySet()) {
			Branch branch = entry.getKey();
			List<Release> branchReleases = entry.getValue();

			branchReleases.sort(Comparator.comparing(Release::getPublishedAt));

			Branch branchWithPRs = branchesWithPullRequests.stream()
					.filter(b -> b.getId().equals(branch.getId()))
					.findFirst()
					.orElse(branch);

			for (Release release : branchReleases) {
				Set<Commit> newCommits = new HashSet<>();
				Set<PullRequest> newPullRequests = new HashSet<>();
				Release previousRelease = branchReleases.stream()
						.filter(r -> r.getPublishedAt().isBefore(release.getPublishedAt()))
						.max(Comparator.comparing(Release::getPublishedAt))
						.orElse(null);

				if (previousRelease == null) {
					newCommits.addAll(getCommitsBeforeRelease(branch, release));
				} else {
					newCommits.addAll(getCommitsBetweenReleases(branch, previousRelease, release));
				}

				if (previousRelease == null) {
					newPullRequests.addAll(getPullRequestsBeforeRelease(branchWithPRs, release));
				} else {
					newPullRequests.addAll(getPullRequestsBetweenReleases(branchWithPRs, previousRelease, release));
				}

				Set<ReleaseCommit> newReleaseCommits = newCommits.stream()
						.map(commit -> new ReleaseCommit(release, commit))
						.collect(Collectors.toSet());

				Set<ReleasePullRequest> newReleasePullRequests = newPullRequests.stream()
						.map(pr -> new ReleasePullRequest(release, pr))
						.collect(Collectors.toSet());

				release.setReleaseCommits(newReleaseCommits);
				release.setReleasePullRequests(newReleasePullRequests);

				updatedReleases.add(release);
			}
		}

		return updatedReleases;
	}

	private Set<Commit> getCommitsBeforeRelease(Branch branch, Release release) {
		return branch.getBranchCommits().stream()
				.map(BranchCommit::getCommit)
				.filter(commit -> commit.getCommittedDate().isBefore(release.getPublishedAt()))
				.collect(Collectors.toSet());
	}

	private Set<Commit> getCommitsBetweenReleases(Branch branch, Release previousRelease, Release release) {
		return branch.getBranchCommits().stream()
				.map(BranchCommit::getCommit)
				.filter(commit -> commit.getCommittedDate().isAfter(previousRelease.getPublishedAt())
						&& commit.getCommittedDate().isBefore(release.getPublishedAt()))
				.collect(Collectors.toSet());
	}

	private Set<PullRequest> getPullRequestsBeforeRelease(Branch branch, Release release) {
		return branch.getBranchPullRequests().stream()
				.map(BranchPullRequest::getPullRequest)
				.filter(pr -> pr.getMergedAt().isBefore(release.getPublishedAt()))
				.collect(Collectors.toSet());
	}

	private Set<PullRequest> getPullRequestsBetweenReleases(Branch branch, Release previousRelease, Release release) {
		return branch.getBranchPullRequests().stream()
				.map(BranchPullRequest::getPullRequest)
				.filter(pr -> pr.getMergedAt().isAfter(previousRelease.getPublishedAt())
						&& pr.getMergedAt().isBefore(release.getPublishedAt()))
				.collect(Collectors.toSet());
	}

	private void saveAllReleases(Set<Release> releases) {
		List<Release> savedReleases = releaseRepository.saveAll(releases);
		log.info("Successfully saved {} releases.", savedReleases.size());
	}
}
