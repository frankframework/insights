package org.frankframework.insights.pullrequest;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.common.configuration.properties.GitHubProperties;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequest;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequestRepository;
import org.frankframework.insights.common.entityconnection.pullrequestissue.PullRequestIssue;
import org.frankframework.insights.common.entityconnection.pullrequestissue.PullRequestIssueRepository;
import org.frankframework.insights.common.entityconnection.pullrequestlabel.PullRequestLabel;
import org.frankframework.insights.common.entityconnection.pullrequestlabel.PullRequestLabelRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.mapper.MappingException;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubClientException;
import org.frankframework.insights.issue.Issue;
import org.frankframework.insights.issue.IssueService;
import org.frankframework.insights.label.Label;
import org.frankframework.insights.label.LabelService;
import org.frankframework.insights.milestone.Milestone;
import org.frankframework.insights.milestone.MilestoneService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for managing pull requests.
 * Handles the injection, mapping, and processing of GitHub pull requests into the database.
 */
@Service
@Slf4j
public class PullRequestService {

    private final GitHubClient gitHubClient;
    private final Mapper mapper;
    private final PullRequestRepository pullRequestRepository;
    private final BranchPullRequestRepository branchPullRequestRepository;
    private final BranchService branchService;
    private final MilestoneService milestoneService;
    private final IssueService issueService;
    private final List<String> branchProtectionRegexes;
    private final PullRequestLabelRepository pullRequestLabelRepository;
    private final PullRequestIssueRepository pullRequestIssueRepository;
    private final LabelService labelService;

    private static final Pattern NEW_BRANCH_NAME_PATTERN = Pattern.compile("release/([\\d.]+)");

    public PullRequestService(
            GitHubClient gitHubClient,
            Mapper mapper,
            PullRequestRepository pullRequestRepository,
            BranchPullRequestRepository branchPullRequestRepository,
            BranchService branchService,
            MilestoneService milestoneService,
            IssueService issueService,
            GitHubProperties gitHubProperties,
            PullRequestLabelRepository pullRequestLabelRepository,
            PullRequestIssueRepository pullRequestIssueRepository,
            LabelService labelService) {
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.pullRequestRepository = pullRequestRepository;
        this.branchPullRequestRepository = branchPullRequestRepository;
        this.branchService = branchService;
        this.milestoneService = milestoneService;
        this.issueService = issueService;
        this.branchProtectionRegexes = gitHubProperties.getBranchProtectionRegexes();
        this.pullRequestLabelRepository = pullRequestLabelRepository;
        this.pullRequestIssueRepository = pullRequestIssueRepository;
        this.labelService = labelService;
    }

    /**
     * Injects pull requests from GitHub into the database for all branches.
     * It intelligently handles renamed branches by querying both new and old naming conventions.
     * @throws PullRequestInjectionException if an error occurs during the injection process.
     */
    @Transactional
    public void injectBranchPullRequests() throws PullRequestInjectionException {
        List<Branch> branches = branchService.getAllBranches();
        log.info("Fetched {} branches from the database.", branches.size());
        Set<PullRequestDTO> sortedMasterPullRequests = fetchSortedMasterPullRequests();

        Set<BranchPullRequest> updatedBranchPullRequests = branches.stream()
                .map(branch -> processBranchPullRequests(branch, sortedMasterPullRequests))
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        if (!updatedBranchPullRequests.isEmpty()) {
            saveBranchPullRequests(updatedBranchPullRequests);
        }
    }

    /**
     * Fetches pull requests for a given branch, including historical data if the branch was renamed.
     * If a branch is named `release/X.Y`, it will also query for pull requests targeted at `X.Y-release`.
     *
     * @param branch The branch for which to fetch pull requests.
     * @return A combined set of PullRequestDTOs from both current and historical branch names.
     * @throws GitHubClientException if the API call fails.
     */
    private Set<PullRequestDTO> fetchCombinedPullRequestsForBranch(Branch branch) throws GitHubClientException {
        String currentName = branch.getName();
        Set<PullRequestDTO> currentPullRequests = gitHubClient.getBranchPullRequests(currentName);
        log.info(
                "Fetched {} pull requests for branch with current name: [{}]", currentPullRequests.size(), currentName);

        Set<PullRequestDTO> combinedPullRequests = new HashSet<>(currentPullRequests);

        Matcher matcher = NEW_BRANCH_NAME_PATTERN.matcher(currentName);
        if (matcher.matches()) {
            String version = matcher.group(1);
            String oldName = version + "-release";
            log.info(
                    "Branch [{}] matches historical pattern. Querying for old name [{}] as well.",
                    currentName,
                    oldName);

            Set<PullRequestDTO> historicalPrs = gitHubClient.getBranchPullRequests(oldName);
            log.info("Fetched {} pull requests for historical name [{}]", historicalPrs.size(), oldName);
            combinedPullRequests.addAll(historicalPrs);
        }

        log.info(
                "Total unique pull requests for branch [{}] after combining historical data: {}",
                currentName,
                combinedPullRequests.size());
        return combinedPullRequests;
    }

    /**
     * Fetches and sorts pull requests from the master branch.
     * @return a set of sorted PullRequestDTOs from the master branch
     * @throws PullRequestInjectionException if an error occurs while fetching or sorting pull requests
     */
    private Set<PullRequestDTO> fetchSortedMasterPullRequests() throws PullRequestInjectionException {
        try {
            Set<PullRequestDTO> masterPullRequests =
                    gitHubClient.getBranchPullRequests(branchProtectionRegexes.getFirst());
            return sortPullRequestsByMergedAt(masterPullRequests);
        } catch (Exception e) {
            throw new PullRequestInjectionException("Error while injecting GitHub pull requests of branch master", e);
        }
    }

    /**
     * Processes pull requests for a given branch by fetching, merging with master, enriching, and saving them.
     * @param branch the branch for which to process pull requests
     * @param sortedMasterPullRequests the set of sorted pull requests from the master branch
     * @return a set of BranchPullRequest objects associated with the branch
     */
    private Set<BranchPullRequest> processBranchPullRequests(
            Branch branch, Set<PullRequestDTO> sortedMasterPullRequests) {
        try {
            Set<PullRequestDTO> branchPullRequests = fetchCombinedPullRequestsForBranch(branch);

            if (isPullRequestCountUnchanged(branch, branchPullRequests)) {
                log.info("Pull requests for branch {} are already up-to-date.", branch.getName());
                return Collections.emptySet();
            }

            Set<PullRequestDTO> mergedPullRequests =
                    mergeMasterAndBranchPullRequests(sortedMasterPullRequests, branchPullRequests);
            Set<PullRequest> enrichedPullRequests = enrichAndPersistPullRequests(mergedPullRequests);
            return createBranchPullRequests(branch, enrichedPullRequests);
        } catch (Exception e) {
            log.error("Failed to process pull requests for branch: {}", branch.getName(), e);
            return null;
        }
    }

    private boolean isPullRequestCountUnchanged(Branch branch, Set<PullRequestDTO> branchPullRequests) {
        return branchPullRequests.size() == branchPullRequestRepository.countAllByBranch_Id(branch.getId());
    }

    private Set<PullRequest> enrichAndPersistPullRequests(Set<PullRequestDTO> pullRequestDTOS) throws MappingException {
        Set<PullRequest> mappedPullRequests = mapAndAssignMilestones(pullRequestDTOS);
        Set<PullRequest> savedPullRequests = savePullRequests(mappedPullRequests);
        persistLabelsAndIssues(savedPullRequests, pullRequestDTOS);
        return savedPullRequests;
    }

    private Set<PullRequest> mapAndAssignMilestones(Set<PullRequestDTO> pullRequestDTOS) throws MappingException {
        Set<PullRequest> pullRequests = mapper.toEntity(pullRequestDTOS, PullRequest.class);
        Map<String, PullRequestDTO> pullRequestDtoMap =
                pullRequestDTOS.stream().collect(Collectors.toMap(PullRequestDTO::id, Function.identity()));

        return assignMilestonesToPullRequests(pullRequests, pullRequestDtoMap);
    }

    private void persistLabelsAndIssues(Set<PullRequest> savedPullRequests, Set<PullRequestDTO> pullRequestDTOS) {
        Map<String, PullRequestDTO> pullRequestDtoMap =
                pullRequestDTOS.stream().collect(Collectors.toMap(PullRequestDTO::id, Function.identity()));

        Set<PullRequestLabel> allPullRequestLabels = buildAllPullRequestLabels(savedPullRequests, pullRequestDtoMap);
        Set<PullRequestIssue> allPullRequestIssues = buildAllPullRequestIssues(savedPullRequests, pullRequestDtoMap);

        if (!allPullRequestLabels.isEmpty()) {
            pullRequestLabelRepository.saveAll(allPullRequestLabels);
        }
        if (!allPullRequestIssues.isEmpty()) {
            pullRequestIssueRepository.saveAll(allPullRequestIssues);
        }
    }

    private Set<PullRequestLabel> buildAllPullRequestLabels(
            Set<PullRequest> pullRequests, Map<String, PullRequestDTO> pullRequestDtoMap) {
        Map<String, Label> labelMap = labelService.getAllLabelsMap();
        Set<PullRequestLabel> allLabels = new HashSet<>();
        for (PullRequest pr : pullRequests) {
            PullRequestDTO dto = pullRequestDtoMap.get(pr.getId());
            if (dto != null && dto.hasLabels()) {
                dto.labels().edges().stream()
                        .map(labelDTO -> new PullRequestLabel(
                                pr, labelMap.getOrDefault(labelDTO.node().id(), null)))
                        .filter(prLabel -> prLabel.getLabel() != null)
                        .forEach(allLabels::add);
            }
        }
        return allLabels;
    }

    private Set<PullRequestIssue> buildAllPullRequestIssues(
            Set<PullRequest> pullRequests, Map<String, PullRequestDTO> pullRequestDtoMap) {
        Map<String, Issue> issueMap = issueService.getAllIssuesMap();
        Set<PullRequestIssue> allIssues = new HashSet<>();
        for (PullRequest pr : pullRequests) {
            PullRequestDTO dto = pullRequestDtoMap.get(pr.getId());
            if (dto != null && dto.hasClosingIssuesReferences()) {
                dto.closingIssuesReferences().edges().stream()
                        .filter(Objects::nonNull)
                        .filter(edge -> edge.node() != null)
                        .map(edge -> new PullRequestIssue(
                                pr, issueMap.getOrDefault(edge.node().id(), null)))
                        .filter(prIssue -> prIssue.getIssue() != null)
                        .forEach(allIssues::add);
            }
        }
        return allIssues;
    }

    private Set<PullRequestDTO> mergeMasterAndBranchPullRequests(
            Set<PullRequestDTO> sortedMasterPullRequests, Set<PullRequestDTO> branchPullRequests) {
        Set<PullRequestDTO> sortedBranchPullRequests = sortPullRequestsByMergedAt(branchPullRequests);

        if (sortedBranchPullRequests.isEmpty()) {
            log.warn("Branch has no pull requests yet — skipping merge");
            return Collections.emptySet();
        }

        if (new HashSet<>(sortedMasterPullRequests).equals(new HashSet<>(branchPullRequests))
                || sortedMasterPullRequests.isEmpty()) {
            return sortedMasterPullRequests;
        }

        PullRequestDTO firstBranchPR = sortedBranchPullRequests.iterator().next();

        Set<PullRequestDTO> relevantMasterPRs = sortedMasterPullRequests.stream()
                .filter(masterPR -> !masterPR.mergedAt().isAfter(firstBranchPR.mergedAt()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Integer, PullRequestDTO> deduplicatedPRs = Stream.concat(
                        relevantMasterPRs.stream(), sortedBranchPullRequests.stream())
                .collect(Collectors.toMap(
                        PullRequestDTO::number,
                        Function.identity(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));

        return new LinkedHashSet<>(deduplicatedPRs.values());
    }

    private Set<PullRequestDTO> sortPullRequestsByMergedAt(Set<PullRequestDTO> pullRequests) {
        return pullRequests.stream()
                .sorted(Comparator.comparing(PullRequestDTO::mergedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<PullRequest> assignMilestonesToPullRequests(
            Set<PullRequest> pullRequests, Map<String, PullRequestDTO> pullRequestsDtoMap) {
        Map<String, Milestone> milestoneMap = milestoneService.getAllMilestonesMap();
        for (PullRequest pullRequest : pullRequests) {
            PullRequestDTO pullRequestDTO = pullRequestsDtoMap.get(pullRequest.getId());
            if (pullRequestDTO != null
                    && pullRequestDTO.milestone() != null
                    && pullRequestDTO.milestone().id() != null) {
                pullRequest.setMilestone(
                        milestoneMap.get(pullRequestDTO.milestone().id()));
            }
        }
        return pullRequests;
    }

    private Set<BranchPullRequest> getBranchPullRequestsForBranch(Branch branch) {
        return branchPullRequestRepository.findAllByBranch_Id(branch.getId());
    }

    private Set<BranchPullRequest> createBranchPullRequests(Branch branch, Set<PullRequest> pullRequests) {
        Set<BranchPullRequest> existingBranchPullRequests = getBranchPullRequestsForBranch(branch);
        Map<String, BranchPullRequest> existingMap = existingBranchPullRequests.stream()
                .collect(Collectors.toMap(pr -> buildUniqueKey(branch, pr.getPullRequest()), Function.identity()));
        return pullRequests.stream()
                .map(pullRequest -> getOrCreateBranchPullRequest(branch, pullRequest, existingMap))
                .collect(Collectors.toSet());
    }

    private BranchPullRequest getOrCreateBranchPullRequest(
            Branch branch, PullRequest pullRequest, Map<String, BranchPullRequest> existingPullRequestsMap) {
        String key = buildUniqueKey(branch, pullRequest);
        return existingPullRequestsMap.getOrDefault(key, new BranchPullRequest(branch, pullRequest));
    }

    private String buildUniqueKey(Branch branch, PullRequest pullRequest) {
        return String.format("%s::%s", branch.getId(), pullRequest.getId());
    }

    private void saveBranchPullRequests(Set<BranchPullRequest> branchPullRequests) {
        List<BranchPullRequest> savedBranchPullRequests = branchPullRequestRepository.saveAll(branchPullRequests);
        log.info("Saved {} branch pull requests", savedBranchPullRequests.size());
    }

    private Set<PullRequest> savePullRequests(Set<PullRequest> pullRequests) {
        List<PullRequest> savedPullRequests = pullRequestRepository.saveAll(pullRequests);
        log.info("Saved {} pull requests", savedPullRequests.size());
        return new HashSet<>(savedPullRequests);
    }
}
