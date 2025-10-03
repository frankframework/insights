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
import org.frankframework.insights.pullrequest.PullRequest;
import org.springframework.stereotype.Service;

/**
 * Service class for managing releases.
 * Handles the injection, mapping, and processing of GitHub releases into the database.
 */
@Service
@Slf4j
public class ReleaseService {

    private final GitHubClient gitHubClient;
    private final Mapper mapper;
    private final ReleaseRepository releaseRepository;
    private final BranchService branchService;
    private final ReleasePullRequestRepository releasePullRequestRepository;

    private static final String MASTER_BRANCH_NAME = "master";
    private static final String NIGHTLY_RELEASE_NAME = "nightly";
    private static final int FIRST_RELEASE_INDEX = 0;
    private static final int MAJOR = 1;
    private static final int MINOR = 2;

    /**
     * Constructor for ReleaseService.
     *
     * @param gitHubClient Client for interacting with GitHub API.
     * @param mapper Mapper for converting between DTOs and entities.
     * @param releaseRepository Repository for managing Release entities.
     * @param branchService Service for managing branches.
     * @param releasePullRequestRepository Repository for managing ReleasePullRequest entities.
     */
    public ReleaseService(
            GitHubClient gitHubClient,
            Mapper mapper,
            ReleaseRepository releaseRepository,
            BranchService branchService,
            ReleasePullRequestRepository releasePullRequestRepository) {
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
        try {
            Set<ReleaseDTO> releaseDTOs = gitHubClient.getReleases();
            List<Branch> allBranches = branchService.getAllBranches();
            Map<String, Set<BranchPullRequest>> pullRequestsByBranch =
                    branchService.getBranchPullRequestsByBranches(allBranches);

            Map<Boolean, List<ReleaseDTO>> partitionedReleases =
                    releaseDTOs.stream().collect(Collectors.partitioningBy(ReleaseDTO::isValid));

            List<ReleaseDTO> validReleaseDTOs = partitionedReleases.getOrDefault(true, Collections.emptyList());
            List<ReleaseDTO> invalidReleaseDTOs = partitionedReleases.getOrDefault(false, Collections.emptyList());

            Set<Release> releases = validReleaseDTOs.stream()
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

            Map<String, OffsetDateTime> earliestBetaRCDates =
                    buildEarliestBetaRCDatesMap(validReleaseDTOs, invalidReleaseDTOs);

            processAndAssignPullsAndCommits(releasesByBranch, pullRequestsByBranch, earliestBetaRCDates);
        } catch (Exception e) {
            throw new ReleaseInjectionException("Error injecting GitHub releases.", e);
        }
    }

    /**
     * Builds a map of the earliest beta/RC dates for each valid release.
     * This allows PRs to be assigned to the final release even if they were merged during the beta/RC period.
     *
     * @param validReleaseDTOs The list of valid (non-beta/RC) releases.
     * @param invalidReleaseDTOs The list of beta/RC releases.
     * @return A map of release tag names to their earliest beta/RC publish dates.
     */
    private Map<String, OffsetDateTime> buildEarliestBetaRCDatesMap(
            List<ReleaseDTO> validReleaseDTOs, List<ReleaseDTO> invalidReleaseDTOs) {
        Map<String, OffsetDateTime> earliestBetaRCDates = new HashMap<>();

        for (ReleaseDTO validRelease : validReleaseDTOs) {
            String validTagName = validRelease.tagName();
            if (validTagName == null) continue;

            Optional<String> baseVersion = extractMajorMinor(validTagName);
            if (baseVersion.isEmpty()) continue;

            invalidReleaseDTOs.stream()
                    .filter(invalidRelease -> invalidRelease.tagName() != null
                            && invalidRelease.tagName().startsWith(baseVersion.get()))
                    .map(ReleaseDTO::publishedAt)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .ifPresent(earliestDate -> earliestBetaRCDates.put(validTagName, earliestDate));
        }

        return earliestBetaRCDates;
    }

    /**
     * Maps a ReleaseDTO to a Release entity and associates it with a branch.
     *
     * @param dto The ReleaseDTO to map.
     * @param branches The list of branches to match against.
     * @return The mapped Release entity.
     */
    private Release mapToRelease(ReleaseDTO dto, List<Branch> branches) {
        String releaseName = dto.name();

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
        if (tagName == null) {
            return Optional.empty();
        }
        Matcher matcher = Pattern.compile("v(\\d+)\\.(\\d+)").matcher(tagName);
        return matcher.find() ? Optional.of(matcher.group(MAJOR) + "." + matcher.group(MINOR)) : Optional.empty();
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
     * @param earliestBetaRCDates A map of release tag names to their earliest beta/RC dates.
     */
    private void processAndAssignPullsAndCommits(
            Map<Branch, List<Release>> releasesByBranch,
            Map<String, Set<BranchPullRequest>> pullRequestsByBranch,
            Map<String, OffsetDateTime> earliestBetaRCDates) {

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
                List<Release> sortedReleases = assignToReleases(releases, prs, earliestBetaRCDates);
                masterReleases.add(sortedReleases.getFirst());
            }
        }

        if (masterBranch != null) {
            List<Release> masterOnly = releasesByBranch.getOrDefault(masterBranch, List.of());
            List<Release> combined = new ArrayList<>(masterReleases);
            combined.addAll(masterOnly);
            combined.sort(Comparator.comparing(Release::getPublishedAt));

            Set<BranchPullRequest> masterPRs = pullRequestsByBranch.getOrDefault(masterBranch.getId(), Set.of());

            assignToReleases(combined, masterPRs, earliestBetaRCDates);
        }
    }

    /**
     * Assigns pull requests to releases based on their publication dates.
     * This method sorts the releases and assigns pull requests to each release based on the time window between releases.
     * The first release in the sorted list does not get PRs assigned via this method,
     * it serves as the starting point for the next releases time window.
     *
     * @param releases The list of releases to assign pull requests to.
     * @param prs The set of branch pull requests to consider for assignment.
     * @param earliestBetaRCDates A map of release tag names to their earliest beta/RC dates.
     * @return A list of releases with assigned pull requests.
     */
    private List<Release> assignToReleases(
            List<Release> releases, Set<BranchPullRequest> prs, Map<String, OffsetDateTime> earliestBetaRCDates) {
        releases.sort(getReleaseSortingComparator());

        for (int i = FIRST_RELEASE_INDEX; i < releases.size(); i++) {
            Release current = releases.get(i);

            // The first release in the sorted list does not get PRs assigned via this method.
            // It serves as the starting point for the next releases time window.
            if (i > FIRST_RELEASE_INDEX) {
                OffsetDateTime from = releases.get(i - 1).getPublishedAt();
                // Use the earliest beta/RC date if available, otherwise use the final release date
                OffsetDateTime to = earliestBetaRCDates.getOrDefault(current.getTagName(), current.getPublishedAt());
                assignPullRequests(current, prs, from, to);
            }
        }
        return releases;
    }

    /**
     * Returns a comparator for sorting releases.
     * Nightly releases are prioritized first, followed by releases sorted by their publication date.
     *
     * @return A comparator for sorting releases.
     */
    protected Comparator<Release> getReleaseSortingComparator() {
        return Comparator.comparing((Release release) -> release.getName() != null
                        && release.getName().toLowerCase().contains(NIGHTLY_RELEASE_NAME))
                .thenComparing(Release::getPublishedAt);
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
        Set<ReleaseResponse> releaseResponses = releaseRepository.findAll().stream()
                .map(r -> mapper.toDTO(r, ReleaseResponse.class))
                .collect(Collectors.toSet());

        log.info("Successfully fetched and mapped {} releases from the database", releaseResponses.size());
        return releaseResponses;
    }

    /**
     * Checks if a release with the given ID exists in the database.
     * @param releaseId the ID of the release to check
     * @return the Release object if it exists
     * @throws ReleaseNotFoundException if the release does not exist
     */
    public Release checkIfReleaseExists(String releaseId) throws ReleaseNotFoundException {
        Optional<Release> release = releaseRepository.findById(releaseId);
        if (release.isEmpty()) {
            throw new ReleaseNotFoundException("Release with ID [" + releaseId + "] not found.", null);
        }

        return release.get();
    }
}
