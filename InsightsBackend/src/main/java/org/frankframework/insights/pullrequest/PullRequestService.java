package org.frankframework.insights.pullrequest;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.common.configuration.GitHubProperties;
import org.frankframework.insights.common.entityconnection.PullRequestIssue;
import org.frankframework.insights.common.entityconnection.PullRequestLabel;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequest;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequestRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.issue.Issue;
import org.frankframework.insights.issue.IssueService;
import org.frankframework.insights.label.Label;
import org.frankframework.insights.label.LabelService;
import org.frankframework.insights.milestone.Milestone;
import org.frankframework.insights.milestone.MilestoneService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PullRequestService {

    private final GitHubClient gitHubClient;
    private final Mapper mapper;
    private final BranchPullRequestRepository branchPullRequestRepository;
    private final BranchService branchService;
    private final LabelService labelService;
    private final MilestoneService milestoneService;
    private final IssueService issueService;
    private final List<String> branchProtectionRegexes;

    public PullRequestService(
            GitHubClient gitHubClient,
            Mapper mapper,
            BranchPullRequestRepository branchPullRequestRepository,
            BranchService branchService,
            LabelService labelService,
            MilestoneService milestoneService,
            IssueService issueService,
            GitHubProperties gitHubProperties) {
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.branchPullRequestRepository = branchPullRequestRepository;
        this.branchService = branchService;
        this.labelService = labelService;
        this.milestoneService = milestoneService;
        this.issueService = issueService;
        this.branchProtectionRegexes = gitHubProperties.getBranchProtectionRegexes();
    }

    public void injectBranchPullRequests() throws PullRequestInjectionException {
        List<Branch> branches = branchService.getAllBranches();
        log.info("Fetched {} branches", branches.size());

        Set<PullRequestDTO> sortedMasterPullRequests = fetchSortedMasterPullRequests();

        Set<Branch> updatedBranches = branches.stream()
                .map(branch -> {
                    try {
                        return getPullRequestsForBranch(branch, sortedMasterPullRequests);
                    } catch (PullRequestInjectionException e) {
                        log.error("Failed to update pull requests for branch: {}", branch.getName(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (!updatedBranches.isEmpty()) {
            branchService.saveBranches(updatedBranches);
        }

        log.info("Updated pull requests for {} branches", updatedBranches.size());
    }

    private Set<PullRequestDTO> fetchSortedMasterPullRequests() throws PullRequestInjectionException {
        try {
            Set<PullRequestDTO> masterPullRequests =
                    gitHubClient.getBranchPullRequests(branchProtectionRegexes.getFirst());
            return sortPullRequestsByMergedAt(masterPullRequests);
        } catch (Exception e) {
            throw new PullRequestInjectionException("Error while injecting GitHub pull requests of branch master", e);
        }
    }

    private Branch getPullRequestsForBranch(Branch branch, Set<PullRequestDTO> sortedMasterPullRequests)
            throws PullRequestInjectionException {
        try {
            Set<PullRequestDTO> branchPullRequestDTOS = gitHubClient.getBranchPullRequests(branch.getName());
            Set<PullRequestDTO> mergedPullRequests =
                    mergeMasterAndBranchPullRequests(sortedMasterPullRequests, branchPullRequestDTOS);
            Set<PullRequest> pullRequests = mapper.toEntity(mergedPullRequests, PullRequest.class);

            Map<String, PullRequestDTO> pullRequestDtoMap =
                    mergedPullRequests.stream().collect(Collectors.toMap(PullRequestDTO::id, dto -> dto));

            Set<PullRequest> enrichedPullRequests = assignSubPropertiesToPullRequests(pullRequests, pullRequestDtoMap);

            Set<BranchPullRequest> existingBranchPullRequests = new HashSet<>(getBranchPullRequestsForBranch(branch));
            Set<BranchPullRequest> updatedBranchPullRequests =
                    processBranchPullRequests(branch, enrichedPullRequests, existingBranchPullRequests);

            existingBranchPullRequests.clear();
            existingBranchPullRequests.addAll(updatedBranchPullRequests);
            branch.setBranchPullRequests(existingBranchPullRequests);

            log.info("Updated branch {} with {} pull request(s)", branch.getName(), updatedBranchPullRequests.size());
            return branch;
        } catch (Exception e) {
            throw new PullRequestInjectionException("Error while injecting GitHub pull requests", e);
        }
    }

    private Set<PullRequest> assignSubPropertiesToPullRequests(
            Set<PullRequest> pullRequests, Map<String, PullRequestDTO> pullRequestsDtoMap) {
        Map<String, Label> labelMap = labelService.getAllLabelsMap();
        Map<String, Milestone> milestoneMap = milestoneService.getAllMilestonesMap();
        Map<String, Issue> issueMap = issueService.getAllIssuesMap();

        pullRequests.forEach(pullRequest -> {
            PullRequestDTO pullRequestDTO = pullRequestsDtoMap.get(pullRequest.getId());
            if (pullRequestDTO != null) {
                if (pullRequestDTO.labels() != null) {
                    Set<PullRequestLabel> pullRequestLabels = pullRequestDTO.labels().getEdges().stream()
                            .map(labelDTO -> new PullRequestLabel(
                                    pullRequest, labelMap.getOrDefault(labelDTO.getNode().id, null)))
                            .filter(issueLabel -> issueLabel.getLabel() != null)
                            .collect(Collectors.toSet());
                    pullRequest.setPullRequestLabels(pullRequestLabels);
                }

                if (pullRequestDTO.milestone() != null
                        && pullRequestDTO.milestone().id() != null) {
                    pullRequest.setMilestone(
                            milestoneMap.get(pullRequestDTO.milestone().id()));
                }

                if (pullRequestDTO.closingIssuesReferences() != null
                        && pullRequestDTO.closingIssuesReferences().getEdges() != null
                        && !pullRequestDTO.closingIssuesReferences().getEdges().isEmpty()) {

                    Set<PullRequestIssue> pullRequestIssues =
                            pullRequestDTO.closingIssuesReferences().getEdges().stream()
                                    .filter(issueDTO -> issueDTO != null && issueDTO.getNode() != null)
                                    .map(issueDTO -> new PullRequestIssue(
                                            pullRequest,
                                            issueMap.getOrDefault(
                                                    issueDTO.getNode().id(), null)))
                                    .filter(pullRequestIssue -> pullRequestIssue.getIssue() != null)
                                    .collect(Collectors.toSet());

                    pullRequest.setPullRequestIssues(pullRequestIssues);
                }
            }
        });
        return pullRequests;
    }

    private Set<BranchPullRequest> getBranchPullRequestsForBranch(Branch branch) {
        return branchPullRequestRepository.findBranchPullRequestByBranchId(branch.getId());
    }

    private Set<PullRequestDTO> mergeMasterAndBranchPullRequests(
            Set<PullRequestDTO> sortedMasterPullRequests, Set<PullRequestDTO> branchPullRequests) {
        Set<PullRequestDTO> sortedBranchPullRequests = sortPullRequestsByMergedAt(branchPullRequests);

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

    private Set<BranchPullRequest> processBranchPullRequests(
            Branch branch, Set<PullRequest> pullRequests, Set<BranchPullRequest> existingBranchPullRequests) {
        Map<String, BranchPullRequest> existingPullRequestsMap = existingBranchPullRequests.stream()
                .collect(Collectors.toMap(bp -> buildUniqueKey(bp.getBranch(), bp.getPullRequest()), bp -> bp));

        return pullRequests.stream()
                .map(pullRequest -> getOrCreateBranchPullRequest(branch, pullRequest, existingPullRequestsMap))
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
}
