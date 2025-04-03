package org.frankframework.insights.release;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.commit.Commit;
import org.frankframework.insights.common.entityconnection.ReleaseCommit;
import org.frankframework.insights.common.entityconnection.ReleasePullRequest;
import org.frankframework.insights.common.entityconnection.branchcommit.BranchCommit;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequest;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.frankframework.insights.pullrequest.PullRequest;

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
			log.info("Amount of releases found in database: {}", releaseRepository.count());
			log.info("Amount of releases found in GitHub: {}", gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubReleaseCount());

			log.info("Fetching GitHub releases...");
            Set<ReleaseDTO> releaseDTOs = gitHubClient.getReleases();
            List<Branch> branches = branchService.getAllBranchesWithCommits();

            List<Release> releasesWithBranches = releaseDTOs.stream()
                    .map(dto -> createReleaseWithBranch(dto, branches))
                    .filter(Objects::nonNull)
                    .toList();

            if (releasesWithBranches.isEmpty()) {
                log.info("No releases with detected base branch to inject.");
                return;
            }

			Set<Release> releasesWithNewCommitsAndPRs = assignNewCommitsAndPullRequestsToReleases(releasesWithBranches);

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
            log.warn("No matching branch found for release '{}'.", release.getTagName());
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

	private Set<Release> assignNewCommitsAndPullRequestsToReleases(List<Release> releases) {
		Map<Branch, List<Release>> releasesByBranch = groupReleasesByBranch(releases);
		Set<Release> updatedReleases = new HashSet<>();

		for (Map.Entry<Branch, List<Release>> entry : releasesByBranch.entrySet()) {
			updatedReleases.addAll(processReleasesForBranch(entry.getKey(), entry.getValue()));
		}

		return updatedReleases;
	}

	private Map<Branch, List<Release>> groupReleasesByBranch(List<Release> releases) {
		return releases.stream()
				.filter(release -> release.getBranch() != null)
				.collect(Collectors.groupingBy(Release::getBranch));
	}

	private Set<Release> processReleasesForBranch(Branch branch, List<Release> branchReleases) {
		Set<Release> updatedReleases = new HashSet<>();
		branchReleases.sort(Comparator.comparing(Release::getPublishedAt));
		TreeSet<Release> sortedReleases = new TreeSet<>(Comparator.comparing(Release::getPublishedAt));

		for (Release release : branchReleases) {
			processRelease(branch, release, sortedReleases, updatedReleases);
		}
		return updatedReleases;
	}

	private void processRelease(Branch branch, Release release, TreeSet<Release> sortedReleases, Set<Release> updatedReleases) {
		Release previousRelease = sortedReleases.lower(release);

		Set<Commit> newCommits = findNewCommits(branch, previousRelease, release);
		Set<PullRequest> newPullRequests = findNewPullRequests(branch, previousRelease, release);

		release.setReleaseCommits(mapToReleaseCommits(release, newCommits));
		release.setReleasePullRequests(mapToReleasePullRequests(release, newPullRequests));

		updatedReleases.add(release);
		sortedReleases.add(release);
	}

	private Set<Commit> findNewCommits(Branch branch, Release previousRelease, Release release) {
		return branch.getBranchCommits().stream()
				.map(BranchCommit::getCommit)
				.filter(commit -> isInReleaseRange(commit.getCommittedDate(), previousRelease, release))
				.collect(Collectors.toSet());
	}

	private Set<PullRequest> findNewPullRequests(Branch branch, Release previousRelease, Release release) {
		return branch.getBranchPullRequests().stream()
				.map(BranchPullRequest::getPullRequest)
				.filter(pr -> isInReleaseRange(pr.getMergedAt(), previousRelease, release))
				.collect(Collectors.toSet());
	}

	private boolean isInReleaseRange(OffsetDateTime date, Release previousRelease, Release release) {
		return previousRelease == null
				? !date.isAfter(release.getPublishedAt())
				: date.isAfter(previousRelease.getPublishedAt()) && !date.isAfter(release.getPublishedAt());
	}

	private Set<ReleaseCommit> mapToReleaseCommits(Release release, Set<Commit> commits) {
		return commits.stream()
				.map(commit -> new ReleaseCommit(release, commit))
				.collect(Collectors.toSet());
	}

	private Set<ReleasePullRequest> mapToReleasePullRequests(Release release, Set<PullRequest> pullRequests) {
		return pullRequests.stream()
				.map(pr -> new ReleasePullRequest(release, pr))
				.collect(Collectors.toSet());
	}

	private void saveAllReleases(Set<Release> releases) {
        List<Release> savedReleases = releaseRepository.saveAll(releases);
        log.info("Successfully saved {} releases.", savedReleases.size());
    }
}
