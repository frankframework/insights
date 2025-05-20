package org.frankframework.insights.release;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.commit.Commit;
import org.frankframework.insights.common.entityconnection.branchcommit.BranchCommit;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequest;
import org.frankframework.insights.common.entityconnection.releasecommit.ReleaseCommit;
import org.frankframework.insights.common.entityconnection.releasecommit.ReleaseCommitRepository;
import org.frankframework.insights.common.entityconnection.releasepullrequest.ReleasePullRequest;
import org.frankframework.insights.common.entityconnection.releasepullrequest.ReleasePullRequestRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.frankframework.insights.pullrequest.PullRequest;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReleaseService {

    private final GitHubRepositoryStatisticsService statisticsService;
    private final GitHubClient gitHubClient;
    private final Mapper mapper;
    private final ReleaseRepository releaseRepository;
    private final BranchService branchService;
    private final ReleaseCommitRepository releaseCommitRepository;
    private final ReleasePullRequestRepository releasePullRequestRepository;

    private static final String MASTER_BRANCH_NAME = "master";
    private static final int FIRST_RELEASE_INDEX = 0;

    public ReleaseService(
            GitHubRepositoryStatisticsService statisticsService,
            GitHubClient gitHubClient,
            Mapper mapper,
            ReleaseRepository releaseRepository,
            BranchService branchService,
            ReleaseCommitRepository releaseCommitRepository,
            ReleasePullRequestRepository releasePullRequestRepository) {
        this.statisticsService = statisticsService;
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.releaseRepository = releaseRepository;
        this.branchService = branchService;
        this.releaseCommitRepository = releaseCommitRepository;
        this.releasePullRequestRepository = releasePullRequestRepository;
    }

    public void injectReleases() throws ReleaseInjectionException {
        if (statisticsService.getGitHubRepositoryStatisticsDTO().getGitHubReleaseCount() == releaseRepository.count()) {
            log.info("Releases already exist in the database.");
            return;
        }

        try {
            Set<ReleaseDTO> releaseDTOs = gitHubClient.getReleases();
            List<Branch> allBranches = branchService.getAllBranches();
            Map<String, Set<BranchCommit>> commitsByBranch = branchService.getBranchCommitsByBranches(allBranches);
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

            processAndAssignPullsAndCommits(releasesByBranch, commitsByBranch, pullRequestsByBranch);
        } catch (Exception e) {
            throw new ReleaseInjectionException("Error injecting GitHub releases.", e);
        }
    }

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

	private Optional<String> extractMajorMinor(String tagName) {
		Matcher matcher = Pattern.compile("v(\\d+)\\.(\\d+)").matcher(tagName);
		return matcher.find() ? Optional.of(matcher.group(1) + "." + matcher.group(2)) : Optional.empty();
	}

	private Optional<Branch> findBranchByVersion(List<Branch> branches, String version) {
		return branches.stream()
				.filter(branch -> branch.getName().contains(version))
				.findFirst();
	}

	private Optional<Branch> findMasterBranch(List<Branch> branches) {
		return branches.stream()
				.filter(b -> MASTER_BRANCH_NAME.equalsIgnoreCase(b.getName()))
				.findFirst();
	}

	private void processAndAssignPullsAndCommits(
            Map<Branch, List<Release>> releasesByBranch,
            Map<String, Set<BranchCommit>> commitsByBranch,
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

            Set<BranchCommit> commits = commitsByBranch.getOrDefault(branch.getId(), Set.of());
            Set<BranchPullRequest> prs = pullRequestsByBranch.getOrDefault(branch.getId(), Set.of());

            if (releases.size() == 1) {
                masterReleases.add(releases.getFirst());
            } else {
                List<Release> sortedReleases = assignToReleases(releases, commits, prs);
                masterReleases.add(sortedReleases.getFirst());
            }
        }

        if (masterBranch != null) {
            List<Release> masterOnly = releasesByBranch.getOrDefault(masterBranch, List.of());
            List<Release> combined = new ArrayList<>(masterReleases);
            combined.addAll(masterOnly);
            combined.sort(Comparator.comparing(Release::getPublishedAt));

            Set<BranchCommit> masterCommits = commitsByBranch.getOrDefault(masterBranch.getId(), Set.of());
            Set<BranchPullRequest> masterPRs = pullRequestsByBranch.getOrDefault(masterBranch.getId(), Set.of());

            assignToReleases(combined, masterCommits, masterPRs);
        }
    }

    /**
     * Assigns commits and pull requests to each release based on the time window between the current and previous release.
     * Only releases beyond the first one will receive assignments, as the time range requires a preceding release.
     *
     * @param releases the list of releases on a branch
     * @param commits  the set of branch commits to assign
     * @param prs      the set of branch pull requests to assign
     * @return the list of releases sorted by their published date
     */
    private List<Release> assignToReleases(
            List<Release> releases, Set<BranchCommit> commits, Set<BranchPullRequest> prs) {

        for (int i = FIRST_RELEASE_INDEX; i < releases.size(); i++) {
            Release current = releases.get(i);

            // Skip the first release since there is no previous release to compare to
            if (i > FIRST_RELEASE_INDEX) {
                OffsetDateTime from = releases.get(i - 1).getPublishedAt();
                OffsetDateTime to = current.getPublishedAt();
                assignCommits(current, commits, from, to);
                assignPullRequests(current, prs, from, to);
            }
        }

        return releases.stream()
                .sorted(Comparator.comparing(Release::getPublishedAt))
                .toList();
    }

    private void assignCommits(
            Release release, Set<BranchCommit> branchCommits, OffsetDateTime from, OffsetDateTime to) {
        Set<Commit> commits = branchCommits.stream()
                .map(BranchCommit::getCommit)
                .filter(c -> isInRange(c.getCommittedDate(), from, to))
                .collect(Collectors.toSet());

        if (!commits.isEmpty()) {
            releaseCommitRepository.saveAll(
                    commits.stream().map(c -> new ReleaseCommit(release, c)).collect(Collectors.toSet()));
        }
    }

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

    private boolean isInRange(OffsetDateTime date, OffsetDateTime start, OffsetDateTime end) {
        return date != null && (date.isEqual(start) || date.isAfter(start)) && date.isBefore(end);
    }

    private void saveAllReleases(Set<Release> releases) {
        List<Release> savedReleases = releaseRepository.saveAll(releases);
        log.info("Saved {} releases.", savedReleases.size());
    }

    public Set<ReleaseResponse> getAllReleases() {
        return releaseRepository.findAll().stream()
                .map(r -> mapper.toDTO(r, ReleaseResponse.class))
                .collect(Collectors.toSet());
    }

    public Release checkIfReleaseExists(String releaseId) throws ReleaseNotFoundException {
        return releaseRepository
                .findById(releaseId)
                .orElseThrow(() -> new ReleaseNotFoundException("Release was not found.", null));
    }
}
