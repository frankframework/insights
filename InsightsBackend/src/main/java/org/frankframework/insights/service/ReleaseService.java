package org.frankframework.insights.service;

import java.util.*;
import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.dto.MatchingBranch;
import org.frankframework.insights.dto.ReleaseDTO;
import org.frankframework.insights.mapper.ReleaseMapper;
import org.frankframework.insights.models.Branch;
import org.frankframework.insights.models.Commit;
import org.frankframework.insights.models.Release;
import org.frankframework.insights.repository.ReleaseRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ReleaseService {
	private static final Logger logger = LoggerFactory.getLogger(ReleaseService.class);

	private final GitHubClient gitHubClient;
	private final ReleaseMapper releaseMapper;
	private final ReleaseRepository releaseRepository;
	private final BranchService branchService;

	public ReleaseService(GitHubClient gitHubClient, ReleaseMapper releaseMapper,
						  ReleaseRepository releaseRepository, BranchService branchService) {
		this.gitHubClient = gitHubClient;
		this.releaseMapper = releaseMapper;
		this.releaseRepository = releaseRepository;
		this.branchService = branchService;
	}

	public void injectReleases() {
		if (!releaseRepository.findAll().isEmpty()) {
			return;
		}

		try {
			Set<ReleaseDTO> releaseDTOs = gitHubClient.getReleases();
			Set<Release> releases = releaseMapper.toEntity(releaseDTOs);

			Map<String, MatchingBranch> releaseBranches = stripReleaseBranches(releaseDTOs);

			for (Release release : releases) {
				MatchingBranch matchingBranch = releaseBranches.get(release.getTagName());
				if (matchingBranch == null) {
					logger.warn("No matching branch found for release {}", release.getTagName());
					continue;
				}

				release.setBranch(matchingBranch.branch());
				release.setCommits(matchingBranch.commits());
				logger.debug("Saving release {} with {} commits", release.getTagName(), matchingBranch.commits().size());
				saveRelease(release);
			}
		} catch (Exception e) {
			logger.error("Error injecting releases", e);
			throw new RuntimeException(e);
		}
	}

	private Map<String, MatchingBranch> stripReleaseBranches(Set<ReleaseDTO> releaseDTOs) {
		List<Branch> branches = branchService.getAllBranches();
		Map<String, MatchingBranch> releaseBranches = new HashMap<>();

		for (ReleaseDTO releaseDTO : releaseDTOs) {
			Set<Branch> matchingBranches = findMatchingBranches(branches, releaseDTO.getTagCommit().getOid());
			MatchingBranch calculatedBranch = weighBestMatchingBranch(matchingBranches, releaseDTO.getTagCommit().getOid());

			if (calculatedBranch == null) {
				logger.warn("No best match found for release {} with commit {}", releaseDTO.getTagName(), releaseDTO.getTagCommit().getOid());
			}

			releaseBranches.put(releaseDTO.getTagName(),
					calculatedBranch != null ? calculatedBranch : new MatchingBranch(null, Collections.emptySet()));
		}

		return releaseBranches;
	}

	private Set<Branch> findMatchingBranches(List<Branch> branches, String commitOid) {
		Set<Branch> matchingBranches = new HashSet<>();
		for (Branch branch : branches) {
			if (branchService.doesBranchContainCommit(branch, commitOid)) {
				matchingBranches.add(branch);
			}
		}
		return matchingBranches;
	}

	private MatchingBranch weighBestMatchingBranch(Set<Branch> branches, String releaseCommitOid) {
		Map<Branch, Float> matchingBranches = new HashMap<>();
		Map<Branch, Set<Commit>> branchReleaseCommits = new HashMap<>();

		for (Branch branch : branches) {
			List<Commit> sortedCommits = branch.getCommits().stream()
					.sorted(Comparator.comparing(Commit::getCommittedDate))
					.toList();

			List<Commit> commitsBeforeRelease = sortedCommits.stream()
					.takeWhile(commit -> !commit.getOid().equals(releaseCommitOid))
					.toList();

			branchReleaseCommits.put(branch, new HashSet<>(commitsBeforeRelease));

			if ("master".equals(branch.getName())) {
				logger.debug("Master branch selected for release commit {}", releaseCommitOid);
				return new MatchingBranch(branch, new HashSet<>(commitsBeforeRelease));
			}

			float ratio = calculateBestMatchingBranch(sortedCommits.size(), commitsBeforeRelease.size());
			matchingBranches.put(branch, ratio);
		}

		return matchingBranches.entrySet().stream()
				.max(Map.Entry.comparingByValue())
				.map(entry -> new MatchingBranch(entry.getKey(), branchReleaseCommits.get(entry.getKey())))
				.orElse(null);
	}

	private float calculateBestMatchingBranch(int totalAmountCommits, int amountOfCommitsBeforeReleaseCommit) {
		return amountOfCommitsBeforeReleaseCommit == 0 ? 0 : (float) totalAmountCommits / amountOfCommitsBeforeReleaseCommit;
	}

	private void saveRelease(Release release) {
		releaseRepository.save(release);
	}
}
