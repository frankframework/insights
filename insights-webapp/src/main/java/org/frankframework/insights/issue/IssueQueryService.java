package org.frankframework.insights.issue;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabel;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabelRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.issueprojects.IssuePriorityResponse;
import org.frankframework.insights.issueprojects.IssueStateResponse;
import org.frankframework.insights.issuetype.IssueTypeResponse;
import org.frankframework.insights.label.LabelQueryService;
import org.frankframework.insights.label.LabelResponse;
import org.frankframework.insights.milestone.Milestone;
import org.frankframework.insights.milestone.MilestoneNotFoundException;
import org.frankframework.insights.milestone.MilestoneQueryService;
import org.frankframework.insights.milestone.MilestoneResponse;
import org.frankframework.insights.release.Release;
import org.frankframework.insights.release.ReleaseNotFoundException;
import org.frankframework.insights.release.ReleaseQueryService;
import org.springframework.stereotype.Service;

/**
 * Service class for reading issues from the database.
 * Filling the issue tables is the responsibility of the data import module.
 */
@Service
@Slf4j
public class IssueQueryService {
    private static final String ISSUE_TYPE_EPIC_NAME = "Epic";
    private static final double DEFAULT_POINTS = 3.0;

    private final Mapper mapper;
    private final IssueRepository issueRepository;
    private final IssueLabelRepository issueLabelRepository;
    private final MilestoneQueryService milestoneQueryService;
    private final LabelQueryService labelQueryService;
    private final ReleaseQueryService releaseQueryService;

    public IssueQueryService(
            Mapper mapper,
            IssueRepository issueRepository,
            IssueLabelRepository issueLabelRepository,
            MilestoneQueryService milestoneQueryService,
            LabelQueryService labelQueryService,
            ReleaseQueryService releaseQueryService) {
        this.mapper = mapper;
        this.issueRepository = issueRepository;
        this.issueLabelRepository = issueLabelRepository;
        this.milestoneQueryService = milestoneQueryService;
        this.labelQueryService = labelQueryService;
        this.releaseQueryService = releaseQueryService;
    }

    /**
     * Fetches all root issues associated with a specific release ID.
     * @param releaseId the ID of the release to fetch issues for
     * @return Set of root issues associated with the release (without sub-issues or labels)
     * @throws ReleaseNotFoundException if the release is not found
     */
    public Set<Issue> getRootIssuesByReleaseId(String releaseId) throws ReleaseNotFoundException {
        Release release = releaseQueryService.checkIfReleaseExists(releaseId);
        Set<Issue> allIssues = issueRepository.findIssuesByReleaseId(release.getId());
        return filterRootIssues(allIssues);
    }

    /**
     * Fetches all issues associated with a specific release ID.
     * @param releaseId the ID of the release to fetch issues for
     * @return Set of issues associated with the release, including sub-issues and labels
     */
    public Set<IssueResponse> getIssuesByReleaseId(String releaseId) throws ReleaseNotFoundException {
        Release release = releaseQueryService.checkIfReleaseExists(releaseId);
        Set<Issue> allIssues = issueRepository.findIssuesByReleaseId(release.getId());
        Set<Issue> rootIssues = filterRootIssues(allIssues);
        return buildIssueResponseTree(rootIssues);
    }

    /**
     * Fetches all issues associated with a specific milestone ID.
     * @param milestoneId the ID of the milestone to fetch issues for
     * @return Set of issues associated with the milestone, including sub-issues and labels
     * @throws MilestoneNotFoundException if the milestone does not exist
     */
    public Set<IssueResponse> getIssuesByMilestoneId(String milestoneId) throws MilestoneNotFoundException {
        Milestone milestone = milestoneQueryService.checkIfMilestoneExists(milestoneId);
        Set<Issue> allIssues = issueRepository.findDistinctByMilestoneId(milestone.getId());
        Set<Issue> rootIssues = filterRootIssues(allIssues);
        return buildIssueResponseTree(rootIssues);
    }

    /**
     * Fetches all epic issues that are planned for the future (i.e., with a due date after the current date).
     * @return Set of future epic issues, including sub-issues and labels
     */
    public Set<IssueResponse> getFutureEpicIssues() {
        Set<Issue> futureEpicIssues = issueRepository.findIssuesByIssueTypeNameAndMilestoneIsNull(ISSUE_TYPE_EPIC_NAME);
        return buildIssueResponseTreeWithoutFiltering(futureEpicIssues);
    }

    /**
     * Filters out issues that are sub-issues of other issues in the set.
     * @param issues the set of all issues
     * @return a set of root issues (issues that are not sub-issues of other issues in the set)
     */
    private Set<Issue> filterRootIssues(Set<Issue> issues) {
        Set<String> allSubIssueIds = issues.stream()
                .filter(issue -> issue.getSubIssues() != null)
                .flatMap(issue -> issue.getSubIssues().stream())
                .map(Issue::getId)
                .collect(Collectors.toSet());

        return issues.stream()
                .filter(issue -> !allSubIssueIds.contains(issue.getId()))
                .collect(Collectors.toSet());
    }

    /**
     * Builds a tree of IssueResponse objects from the given set of root issues,
     * @param rootIssues the set of root issues to build the tree from
     * @return a set of IssueResponse objects representing the root issues and their sub-issues, with labels included
     */
    private Set<IssueResponse> buildIssueResponseTree(Set<Issue> rootIssues) {
        Set<String> allIds = collectAllIssueIdsRecursively(rootIssues);
        Map<String, Set<LabelResponse>> labelsMap = fetchLabelsForIssueIds(allIds);
        return rootIssues.stream()
                .map(issue -> mapIssueTreeWithLabels(issue, labelsMap))
                .filter(this::hasRelevantLabelsRecursively)
                .collect(Collectors.toSet());
    }

    /**
     * Builds a tree of IssueResponse objects without filtering by labels.
     * @param rootIssues the set of root issues to build the tree from
     * @return a set of IssueResponse objects representing the root issues and their sub-issues, with labels included
     */
    private Set<IssueResponse> buildIssueResponseTreeWithoutFiltering(Set<Issue> rootIssues) {
        Set<String> allIds = collectAllIssueIdsRecursively(rootIssues);
        Map<String, Set<LabelResponse>> labelsMap = fetchLabelsForIssueIds(allIds);
        return rootIssues.stream()
                .map(issue -> mapIssueTreeWithLabels(issue, labelsMap))
                .collect(Collectors.toSet());
    }

    /**
     * Recursively collects all issue IDs from a set of issues and their sub-issues.
     * @param issues the set of issues to collect IDs from
     * @return a set of all issue IDs, including those from sub-issues
     */
    private Set<String> collectAllIssueIdsRecursively(Set<Issue> issues) {
        return issues.stream().flatMap(this::flattenIssueIds).collect(Collectors.toSet());
    }

    /**
     * Recursively flattens an issue and its sub-issues into a stream of issue IDs.
     * @param issue the issue to flatten
     * @return a stream of issue IDs, including the ID of the issue itself and those of its sub-issues
     */
    private Stream<String> flattenIssueIds(Issue issue) {
        return Stream.concat(
                Stream.of(issue.getId()),
                issue.getSubIssues() == null
                        ? Stream.empty()
                        : issue.getSubIssues().stream().flatMap(this::flattenIssueIds));
    }

    /**
     * Fetches labels for a set of issue IDs.
     * @param issueIds the set of issue IDs to fetch labels for
     * @return a map of issue IDs to sets of LabelResponse objects
     */
    private Map<String, Set<LabelResponse>> fetchLabelsForIssueIds(Set<String> issueIds) {
        Set<IssueLabel> labels = issueLabelRepository.findAllByIssue_IdIn(new ArrayList<>(issueIds));
        return labels.stream()
                .filter(l -> labelQueryService.isLabelIncluded(l.getLabel()))
                .collect(Collectors.groupingBy(
                        l -> l.getIssue().getId(),
                        Collectors.mapping(l -> mapper.toDTO(l.getLabel(), LabelResponse.class), Collectors.toSet())));
    }

    /**
     * Maps an issue and its sub-issues to an IssueResponse object,
     * @param issue the issue to map
     * @param labelsMap a map of issue IDs to sets of LabelResponse objects
     * @return an IssueResponse object representing the issue, including its labels and sub-issues
     */
    private IssueResponse mapIssueTreeWithLabels(Issue issue, Map<String, Set<LabelResponse>> labelsMap) {
        IssueResponse response = mapper.toDTO(issue, IssueResponse.class);
        mapMilestoneToResponse(issue, response);
        mapIssueTypeToResponse(issue, response);
        mapIssuePriorityToResponse(issue, response);
        mapIssueStateToResponse(issue, response);

        response.setLabels(labelsMap.getOrDefault(issue.getId(), Set.of()));
        response.setSubIssues(mapSubIssuesToResponses(issue, labelsMap));

        double totalPoints = calculateTotalPoints(issue, response);
        response.setPoints(totalPoints);

        return response;
    }

    /**
     * Maps the milestone of an issue to the response.
     * @param issue the issue to map
     * @param response the response to set the milestone on
     */
    private void mapMilestoneToResponse(Issue issue, IssueResponse response) {
        if (issue.getMilestone() != null) {
            response.setMilestone(mapper.toDTO(issue.getMilestone(), MilestoneResponse.class));
        }
    }

    /**
     * Maps the issue type of an issue to the response.
     * @param issue the issue to map
     * @param response the response to set the issue type on
     */
    private void mapIssueTypeToResponse(Issue issue, IssueResponse response) {
        if (issue.getIssueType() != null) {
            response.setIssueType(mapper.toDTO(issue.getIssueType(), IssueTypeResponse.class));
        }
    }

    /**
     * Maps the issue priority of an issue to the response.
     * @param issue the issue to map
     * @param response the response to set the issue priority on
     */
    private void mapIssuePriorityToResponse(Issue issue, IssueResponse response) {
        if (issue.getIssuePriority() != null) {
            response.setIssuePriority(mapper.toDTO(issue.getIssuePriority(), IssuePriorityResponse.class));
        }
    }

    /**
     * Maps the issue state of an issue to the response.
     * @param issue the issue to map
     * @param response the response to set the issue state on
     */
    private void mapIssueStateToResponse(Issue issue, IssueResponse response) {
        if (issue.getIssueState() != null) {
            response.setIssueState(mapper.toDTO(issue.getIssueState(), IssueStateResponse.class));
        }
    }

    /**
     * Maps the sub-issues of an issue to a set of IssueResponse objects,
     * @param issue the issue whose sub-issues are to be mapped
     * @param labelsMap a map of issue IDs to sets of LabelResponse objects
     * @return a set of IssueResponse objects representing the sub-issues of the issue
     */
    private Set<IssueResponse> mapSubIssuesToResponses(Issue issue, Map<String, Set<LabelResponse>> labelsMap) {
        if (issue.getSubIssues() != null && !issue.getSubIssues().isEmpty()) {
            return issue.getSubIssues().stream()
                    .map(sub -> mapIssueTreeWithLabels(sub, labelsMap))
                    .filter(this::hasRelevantLabelsRecursively)
                    .collect(Collectors.toSet());
        } else {
            return Set.of();
        }
    }

    /**
     * Checks if an issue or any of its sub-issues have relevant labels.
     * @param issueResponse the issue response to check
     * @return true if the issue or any of its sub-issues have at least one relevant label
     */
    private boolean hasRelevantLabelsRecursively(IssueResponse issueResponse) {
        if (issueResponse.getLabels() != null && !issueResponse.getLabels().isEmpty()) {
            return true;
        }
        if (issueResponse.getSubIssues() != null
                && !issueResponse.getSubIssues().isEmpty()) {
            return issueResponse.getSubIssues().stream().anyMatch(this::hasRelevantLabelsRecursively);
        }
        return false;
    }

    /**
     * Calculates the total points for an issue, including its own points and all sub-issue points.
     * Uses DEFAULT_POINTS (3.0) for issues without assigned points.
     * @param issue the issue entity
     * @param response the issue response with mapped sub-issues
     * @return the total points for the issue and all its sub-issues
     */
    private double calculateTotalPoints(Issue issue, IssueResponse response) {
        double issuePoints = issue.getPoints() != null ? issue.getPoints() : DEFAULT_POINTS;

        if (response.getSubIssues() != null && !response.getSubIssues().isEmpty()) {
            double subIssuesPoints = response.getSubIssues().stream()
                    .mapToDouble(IssueResponse::getPoints)
                    .sum();
            return issuePoints + subIssuesPoints;
        }

        return issuePoints;
    }
}
