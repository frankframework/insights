package org.frankframework.insights.pullrequest;

import java.util.*;
import java.util.function.Function;
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
import org.frankframework.insights.common.helper.IssueLabelHelperService;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.issue.Issue;
import org.frankframework.insights.issue.IssueService;
import org.frankframework.insights.label.Label;
import org.frankframework.insights.milestone.Milestone;
import org.frankframework.insights.milestone.MilestoneService;
import org.springframework.stereotype.Service;

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
    private final IssueLabelHelperService issueLabelHelperService;

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
            IssueLabelHelperService issueLabelHelperService) {
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
        this.issueLabelHelperService = issueLabelHelperService;
    }

    public void injectBranchPullRequests() throws PullRequestInjectionException {
        List<Branch> branches = branchService.getAllBranches();
        log.info("Fetched {} branches", branches.size());

        Set<PullRequestDTO> sortedMasterPullRequests = fetchSortedMasterPullRequests();

        Set<BranchPullRequest> updatedBranchPullRequests = branches.stream()
                .map(branch -> {
                    try {
                        return getPullRequestsForBranch(branch, sortedMasterPullRequests);
                    } catch (PullRequestInjectionException e) {
                        log.error("Failed to update pull requests for branch: {}", branch.getName(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        if (!updatedBranchPullRequests.isEmpty()) {
            branchPullRequestRepository.saveAll(updatedBranchPullRequests);
        }

        log.info("Updated pull requests for {} branches", updatedBranchPullRequests.size());
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

    private Set<BranchPullRequest> getPullRequestsForBranch(Branch branch, Set<PullRequestDTO> sortedMasterPullRequests)
            throws PullRequestInjectionException {
        try {
            Set<PullRequestDTO> pullRequestDTOS = gitHubClient.getBranchPullRequests(branch.getName());

            if (pullRequestDTOS.size() == branchPullRequestRepository.countAllByBranch_Id(branch.getId())) {
                log.info("Size of pull requests for branch {} are already found in the database.", branch.getName());
                return new HashSet<>();
            }

            Set<PullRequestDTO> mergedPullRequestDTOS =
                    mergeMasterAndBranchPullRequests(sortedMasterPullRequests, pullRequestDTOS);
            Set<PullRequest> pullRequests = mapper.toEntity(mergedPullRequestDTOS, PullRequest.class);

            Map<String, PullRequestDTO> pullRequestDtoMap =
                    mergedPullRequestDTOS.stream().collect(Collectors.toMap(PullRequestDTO::id, dto -> dto));

            Set<PullRequest> pullRequestsWithMilestones =
                    assignMilestonesToPullRequests(pullRequests, pullRequestDtoMap);
            Set<PullRequest> savedPullRequests = savePullRequests(pullRequestsWithMilestones);
            Set<PullRequest> enrichedPullRequests =
                    assignSubPropertiesToPullRequests(savedPullRequests, pullRequestDtoMap);
            Set<BranchPullRequest> existingBranchPullRequests = new HashSet<>(getBranchPullRequestsForBranch(branch));
            return processBranchPullRequests(branch, enrichedPullRequests, existingBranchPullRequests);
        } catch (Exception e) {
            throw new PullRequestInjectionException("Error while injecting GitHub pull requests", e);
        }
    }

    private Set<PullRequest> assignMilestonesToPullRequests(
            Set<PullRequest> pullRequests, Map<String, PullRequestDTO> pullRequestsDtoMap) {
        Map<String, Milestone> milestoneMap = milestoneService.getAllMilestonesMap();

        pullRequests.forEach(pullRequest -> {
            PullRequestDTO pullRequestDTO = pullRequestsDtoMap.get(pullRequest.getId());
            if (pullRequestDTO != null
                    && pullRequestDTO.milestone() != null
                    && pullRequestDTO.milestone().id() != null) {
                pullRequest.setMilestone(
                        milestoneMap.get(pullRequestDTO.milestone().id()));
            }
        });

        return pullRequests;
    }

    private Set<PullRequest> assignSubPropertiesToPullRequests(
            Set<PullRequest> pullRequests, Map<String, PullRequestDTO> pullRequestsDtoMap) {

        Map<String, Label> labelMap = issueLabelHelperService.getAllLabelsMap();
        Map<String, Issue> issueMap = issueService.getAllIssuesMap();

        pullRequests.forEach(pullRequest -> {
            PullRequestDTO dto = pullRequestsDtoMap.get(pullRequest.getId());
            if (dto != null) {
                savePullRequestLabels(pullRequest, dto, labelMap);
                savePullRequestIssues(pullRequest, dto, issueMap);
            }
        });

        return pullRequests;
    }

    private void savePullRequestLabels(PullRequest pullRequest, PullRequestDTO dto, Map<String, Label> labelMap) {
        if (!dto.hasLabels()) return;

        Set<PullRequestLabel> pullRequestLabels = dto.labels().getEdges().stream()
                .map(labelDTO -> new PullRequestLabel(pullRequest, labelMap.getOrDefault(labelDTO.getNode().id, null)))
                .filter(prLabel -> prLabel.getLabel() != null)
                .collect(Collectors.toSet());

        pullRequestLabelRepository.saveAll(pullRequestLabels);
    }

    private void savePullRequestIssues(PullRequest pullRequest, PullRequestDTO dto, Map<String, Issue> issueMap) {
        if (!dto.hasClosingIssuesReferences()) return;

        Set<PullRequestIssue> pullRequestIssues = dto.closingIssuesReferences().getEdges().stream()
                .filter(edge -> edge != null && edge.getNode() != null)
                .map(edge -> new PullRequestIssue(
                        pullRequest, issueMap.getOrDefault(edge.getNode().id(), null)))
                .filter(prIssue -> prIssue.getIssue() != null)
                .collect(Collectors.toSet());

        pullRequestIssueRepository.saveAll(pullRequestIssues);
    }

    private Set<BranchPullRequest> getBranchPullRequestsForBranch(Branch branch) {
        return branchPullRequestRepository.findAllByBranch_Id(branch.getId());
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

    private Set<PullRequest> savePullRequests(Set<PullRequest> pullRequests) {
        List<PullRequest> savedPullRequests = pullRequestRepository.saveAll(pullRequests);
        log.info("Saved {} pull requests", savedPullRequests.size());
        return new HashSet<>(savedPullRequests);
    }
}
