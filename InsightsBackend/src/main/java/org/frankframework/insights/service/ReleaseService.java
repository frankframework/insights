package org.frankframework.insights.service;

import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.dto.MatchingBranch;
import org.frankframework.insights.dto.ReleaseDTO;
import org.frankframework.insights.exceptions.branches.BranchDatabaseException;
import org.frankframework.insights.exceptions.commits.CommitInjectionException;
import org.frankframework.insights.exceptions.releases.ReleaseDatabaseException;
import org.frankframework.insights.exceptions.releases.ReleaseInjectionException;
import org.frankframework.insights.mapper.Mapper;
import org.frankframework.insights.models.Branch;
import org.frankframework.insights.models.Commit;
import org.frankframework.insights.models.Release;
import org.frankframework.insights.repository.ReleaseRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReleaseService {
	private final GitHubClient gitHubClient;
	private final Mapper releaseMapper;
	private final ReleaseRepository releaseRepository;
	private final BranchService branchService;

	public ReleaseService(GitHubClient gitHubClient, Mapper releaseMapper,
						  ReleaseRepository releaseRepository, BranchService branchService) {
		this.gitHubClient = gitHubClient;
		this.releaseMapper = releaseMapper;
		this.releaseRepository = releaseRepository;
		this.branchService = branchService;
	}

	public void injectReleases() throws ReleaseInjectionException {
		if (!releaseRepository.findAll().isEmpty()) {
			log.info("Releases already found in the in database");
			return;
		}

		try {
			log.info("Start injecting GitHub releases");
			Set<ReleaseDTO> releaseDTOs = gitHubClient.getReleases();
			Set<Release> releases = releaseMapper.toEntity(releaseDTOs, Release.class);

			Map<String, MatchingBranch> releaseBranches = stripReleaseBranches(releaseDTOs);

			for (Release release : releases) {
				MatchingBranch matchingBranch = releaseBranches.get(release.getTagName());
				if (matchingBranch == null) {
					log.warn("No matching branch found for release with tagName '{}', skipping release.", release.getTagName());
					continue;
				}

				release.setBranch(matchingBranch.branch());
				release.setCommits(matchingBranch.commits());
				log.info("Saving release '{}' with updated base branch and commits.", release.getTagName());
				saveRelease(release);
			}
		} catch (Exception e) {
			throw new ReleaseInjectionException("Error while injecting GitHub releases, including setting the base branch and new commits for each release", e);
		}
	}

	private Map<String, MatchingBranch> stripReleaseBranches(Set<ReleaseDTO> releaseDTOs) throws BranchDatabaseException {
		List<Branch> branches = branchService.getAllBranches();
		Map<String, MatchingBranch> releaseBranches = new HashMap<>();

		for (ReleaseDTO releaseDTO : releaseDTOs) {
			Set<Branch> matchingBranches = findMatchingBranches(branches, releaseDTO.getTagCommit().getOid());
			MatchingBranch calculatedBranch = weighBestMatchingBranch(matchingBranches, releaseDTO.getTagCommit().getOid());

			if (calculatedBranch == null) {
				log.warn("No matching branch found for release '{}'.", releaseDTO.getTagName());
			}

			releaseBranches.put(releaseDTO.getTagName(),
					calculatedBranch != null ? calculatedBranch : new MatchingBranch(null, Collections.emptySet()));
		}

		return releaseBranches;
	}

	private Set<Branch> findMatchingBranches(List<Branch> branches, String commitOid) throws BranchDatabaseException {
		Set<Branch> matchingBranches = new HashSet<>();
		for (Branch branch : branches) {
			if (branchService.doesBranchContainCommit(branch, commitOid)) {
				matchingBranches.add(branch);
			}
		}
		log.debug("Found {} matching branches for release commit '{}'.", matchingBranches.size(), commitOid);
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
				log.info("Found 'master' branch, automatically selecting it as the matching branch for release commit '{}'.", releaseCommitOid);
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
		return (float) amountOfCommitsBeforeReleaseCommit / totalAmountCommits;
	}

	private void saveRelease(Release release) throws ReleaseDatabaseException {
		try {
			releaseRepository.save(release);
			log.debug("Successfully saved release '{}'.", release.getTagName());
		} catch (Exception e) {
			log.error("Error while saving release '{}': {}", release.getTagName(), e.getMessage(), e);
			throw new ReleaseDatabaseException("Error occurred while saving release: " + release.getTagName(), e);
		}
	}
}
