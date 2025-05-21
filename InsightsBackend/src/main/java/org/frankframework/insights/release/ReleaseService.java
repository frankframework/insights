package org.frankframework.insights.release;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequest;
import org.frankframework.insights.common.entityconnection.releasepullrequest.ReleasePullRequest;
import org.frankframework.insights.common.entityconnection.releasepullrequest.ReleasePullRequestRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.frankframework.insights.pullrequest.PullRequest;
import org.springframework.stereotype.Service;

/**
 * Service class for managing releases.
 * Handles the injection, mapping, and processing of GitHub releases into the database.
 */
@Service
@Slf4j
public class ReleaseService {

	private final GitHubRepositoryStatisticsService statisticsService;
	private final GitHubClient gitHubClient;
	private final Mapper mapper;
	private final ReleaseRepository releaseRepository;
	private final BranchService branchService;
	private final ReleasePullRequestRepository releasePullRequestRepository;

	private static final String MASTER_BRANCH_NAME = "master";
	private static final int FIRST_RELEASE_INDEX = 0;

	/**
	 * Constructor for ReleaseService.
	 *
	 * @param statisticsService Service for GitHub repository statistics.
	 * @param gitHubClient Client for interacting with GitHub API.
	 * @param mapper Mapper for converting between DTOs and entities.
	 * @param releaseRepository Repository for managing Release entities.
	 * @param branchService Service for managing branches.
	 * @param releasePullRequestRepository Repository for managing ReleasePullRequest entities.
	 */
	public ReleaseService(
			GitHubRepositoryStatisticsService statisticsService,
			GitHubClient gitHubClient,
			Mapper mapper,
			ReleaseRepository releaseRepository,
			BranchService branchService,
			ReleasePullRequestRepository releasePullRequestRepository) {
		this.statisticsService = statisticsService;
		this.gitHubClient = gitHubClient;
		this.mapper = mapper;
		this.releaseRepository = releaseRepository;
		this.branchService = branchService;
		this.releasePullRequestRepository = releasePullRequestRepository;
	}

	/**
	 * Injects releases from GitHub into the database.
	 * Maps releases to branches and assigns pull requests and commits to them.
	 *
	 * @throws ReleaseInjectionException if an error occurs during the injection process.
	 */
	public void injectReleases() throws ReleaseInjectionException {
		if (statisticsService.getGitHubRepositoryStatisticsDTO().getGitHubReleaseCount() == releaseRepository.count()) {
			log.info("Releases already exist in the database.");
			return;
		}

		try {
			Set<ReleaseDTO> releaseDTOs = gitHubClient.getReleases();
			List<Branch> allBranches = branchService.getAllBranches();
			Map<String, Set<BranchPullRequest>> pullRequestsByBranch =
					branchService.getBranchPullRequestsByBranches(allBranches);

			Set<Release> releases = releaseDTOs.stream()
					.map(dto -> mapToRelease(dto, allBranches))
					.collect(Collectors.toSet());

			if (releases.isEmpty()) {
				log.info("No valid releases found.");
				return;
			}

			saveAllReleases(releases);

			Map<Branch, List<Release>> releasesByBranch = releases.stream()
					.filter(r -> r.getBranch() != null)
					.collect(Collectors.groupingBy(
							Release::getBranch, Collectors.collectingAndThen(Collectors.toList(), list -> list.stream()
									.sorted(Comparator.comparing(Release::getPublishedAt))
									.collect(Collectors.toList()))));

			processAndAssignPullsAndCommits(releasesByBranch, pullRequestsByBranch);
		} catch (Exception e) {
			throw new ReleaseInjectionException("Error injecting GitHub releases.", e);
		}
	}

	/**
	 * Maps a ReleaseDTO to a Release entity and associates it with a branch.
	 *
	 * @param dto The ReleaseDTO to map.
	 * @param branches The list of branches to match against.
	 * @return The mapped Release entity.
	 */
	private Release mapToRelease(ReleaseDTO dto, List<Branch> branches) {
		String releaseName = dto.getName();

		Optional<String> majorMinor = extractMajorMinor(releaseName);
		Optional<Branch> matchingBranch = majorMinor.flatMap(version -> findBranchByVersion(branches, version));

		Branch selectedBranch = matchingBranch.orElse(null);

		if (selectedBranch == null) {
			selectedBranch = findMasterBranch(branches).orElse(null);
		}

		Release release = mapper.toEntity(dto, Release.class);
		release.setBranch(selectedBranch);
		return release;
	}

	/**
	 * Extracts the major and minor version from a release tag name.
	 *
	 * @param tagName The tag name to extract from.
	 * @return An Optional containing the major and minor version, or empty if not found.
	 */
	private Optional<String> extractMajorMinor(String tagName) {
		Matcher matcher = Pattern.compile("v(\\d+)\\.(\\d+)").matcher(tagName);
		return matcher.find() ? Optional.of(matcher.group(1) + "." + matcher.group(2)) : Optional.empty();
	}

	/**
	 * Finds a branch by matching its name with a version string.
	 *
	 * @param branches The list of branches to search.
	 * @param version The version string to match.
	 * @return An Optional containing the matching branch, or empty if not found.
	 */
	private Optional<Branch> findBranchByVersion(List<Branch> branches, String version) {
		return branches.stream()
				.filter(branch -> branch.getName().contains(version))
				.findFirst();
	}

	/**
	 * Finds the master branch from a list of branches.
	 *
	 * @param branches The list of branches to search.
	 * @return An Optional containing the master branch, or empty if not found.
	 */
	private Optional<Branch> findMasterBranch(List<Branch> branches) {
		return branches.stream()
				.filter(b -> MASTER_BRANCH_NAME.equalsIgnoreCase(b.getName()))
				.findFirst();
	}

	/**
	 * Processes and assigns pull requests and commits to releases.
	 *
	 * @param releasesByBranch A map of branches to their associated releases.
	 * @param pullRequestsByBranch A map of branch IDs to their associated pull requests.
	 */
	private void processAndAssignPullsAndCommits(
			Map<Branch, List<Release>> releasesByBranch,
			Map<String, Set<BranchPullRequest>> pullRequestsByBranch) {

		List<Release> masterReleases = new ArrayList<>();
		Branch masterBranch = releasesByBranch.keySet().stream()
				.filter(b -> MASTER_BRANCH_NAME.equalsIgnoreCase(b.getName()))
				.findFirst()
				.orElse(null);

		for (Map.Entry<Branch, List<Release>> entry : releasesByBranch.entrySet()) {
			Branch branch = entry.getKey();
			List<Release> releases = entry.getValue();
			if (MASTER_BRANCH_NAME.equalsIgnoreCase(branch.getName())) continue;

			Set<BranchPullRequest> prs = pullRequestsByBranch.getOrDefault(branch.getId(), Set.of());

			if (releases.size() == 1) {
				masterReleases.add(releases.getFirst());
			} else {
				List<Release> sortedReleases = assignToReleases(releases, prs);
				masterReleases.add(sortedReleases.getFirst());
			}
		}

		if (masterBranch != null) {
			List<Release> masterOnly = releasesByBranch.getOrDefault(masterBranch, List.of());
			List<Release> combined = new ArrayList<>(masterReleases);
			combined.addAll(masterOnly);
			combined.sort(Comparator.comparing(Release::getPublishedAt));

			Set<BranchPullRequest> masterPRs = pullRequestsByBranch.getOrDefault(masterBranch.getId(), Set.of());

			assignToReleases(combined, masterPRs);
		}
	}

	/**
	 * Assigns pull requests to releases based on their time window.
	 *
	 * @param releases The list of releases to assign pull requests to.
	 * @param prs The set of pull requests to assign.
	 * @return The list of releases sorted by their published date.
	 */
	private List<Release> assignToReleases(
			List<Release> releases, Set<BranchPullRequest> prs) {

		for (int i = FIRST_RELEASE_INDEX; i < releases.size(); i++) {
			Release current = releases.get(i);

			// Skip the first release since there is no previous release to compare to
			if (i > FIRST_RELEASE_INDEX) {
				OffsetDateTime from = releases.get(i - 1).getPublishedAt();
				OffsetDateTime to = current.getPublishedAt();
				assignPullRequests(current, prs, from, to);
			}
		}

		return releases.stream()
				.sorted(Comparator.comparing(Release::getPublishedAt))
				.toList();
	}

	/**
	 * Assigns pull requests to a specific release based on a time range.
	 *
	 * @param release The release to assign pull requests to.
	 * @param branchPRs The set of branch pull requests to consider.
	 * @param from The start of the time range.
	 * @param to The end of the time range.
	 */
	private void assignPullRequests(
			Release release, Set<BranchPullRequest> branchPRs, OffsetDateTime from, OffsetDateTime to) {
		Set<PullRequest> prs = branchPRs.stream()
				.map(BranchPullRequest::getPullRequest)
				.filter(p -> isInRange(p.getMergedAt(), from, to))
				.collect(Collectors.toSet());

		if (!prs.isEmpty()) {
			releasePullRequestRepository.saveAll(
					prs.stream().map(p -> new ReleasePullRequest(release, p)).collect(Collectors.toSet()));
		}
	}

	/**
	 * Checks if a date is within a specified range.
	 *
	 * @param date The date to check.
	 * @param start The start of the range.
	 * @param end The end of the range.
	 * @return True if the date is within the range, false otherwise.
	 */
	private boolean isInRange(OffsetDateTime date, OffsetDateTime start, OffsetDateTime end) {
		return date != null && (date.isEqual(start) || date.isAfter(start)) && date.isBefore(end);
	}

	/**
	 * Saves all releases to the database.
	 *
	 * @param releases The set of releases to save.
	 */
	private void saveAllReleases(Set<Release> releases) {
		List<Release> savedReleases = releaseRepository.saveAll(releases);
		log.info("Saved {} releases.", savedReleases.size());
	}

	/**
	 * Retrieves all releases from the database.
	 *
	 * @return A set of ReleaseResponse DTOs representing all releases.
	 */
	public Set<ReleaseResponse> getAllReleases() {
		return releaseRepository.findAll().stream()
				.map(r -> mapper.toDTO(r, ReleaseResponse.class))
				.collect(Collectors.toSet());
	}

	/**
	 * Checks if a release exists in the database by its ID.
	 *
	 * @param releaseId The ID of the release to check.
	 * @return The Release entity if found.
	 * @throws ReleaseNotFoundException if the release is not found.
	 */
	public Release checkIfReleaseExists(String releaseId) throws ReleaseNotFoundException {
		return releaseRepository
				.findById(releaseId)
				.orElseThrow(() -> new ReleaseNotFoundException("Release was not found.", null));
	}
}
