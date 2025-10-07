package org.frankframework.insights.issue;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabel;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabelRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubNodeDTO;
import org.frankframework.insights.issuePriority.IssuePriority;
import org.frankframework.insights.issuePriority.IssuePriorityResponse;
import org.frankframework.insights.issuePriority.IssuePriorityService;
import org.frankframework.insights.issuetype.IssueType;
import org.frankframework.insights.issuetype.IssueTypeResponse;
import org.frankframework.insights.issuetype.IssueTypeService;
import org.frankframework.insights.label.Label;
import org.frankframework.insights.label.LabelResponse;
import org.frankframework.insights.label.LabelService;
import org.frankframework.insights.milestone.Milestone;
import org.frankframework.insights.milestone.MilestoneNotFoundException;
import org.frankframework.insights.milestone.MilestoneResponse;
import org.frankframework.insights.milestone.MilestoneService;
import org.frankframework.insights.release.Release;
import org.frankframework.insights.release.ReleaseNotFoundException;
import org.frankframework.insights.release.ReleaseService;
import org.springframework.stereotype.Service;

/**
 * Service class for managing issues.
 * Handles the injection, mapping, and processing of GitHub issues into the database.
 */
@Service
@Slf4j
public class IssueService {
    private static final String ISSUE_TYPE_EPIC_NAME = "Epic";
    private static final double DEFAULT_POINTS = 3.0;

    private final GitHubClient gitHubClient;
    private final Mapper mapper;
    private final IssueRepository issueRepository;
    private final IssueLabelRepository issueLabelRepository;
    private final MilestoneService milestoneService;
    private final IssueTypeService issueTypeService;
    private final LabelService labelService;
    private final IssuePriorityService issuePriorityService;
    private final ReleaseService releaseService;

    public IssueService(
            GitHubClient gitHubClient,
            Mapper mapper,
            IssueRepository issueRepository,
            IssueLabelRepository issueLabelRepository,
            MilestoneService milestoneService,
            IssueTypeService issueTypeService,
            LabelService labelService,
            IssuePriorityService issuePriorityService,
            ReleaseService releaseService) {
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.issueRepository = issueRepository;
        this.issueLabelRepository = issueLabelRepository;
        this.milestoneService = milestoneService;
        this.issueTypeService = issueTypeService;
        this.labelService = labelService;
        this.issuePriorityService = issuePriorityService;
        this.releaseService = releaseService;
    }

    /**
     * Injects issues from GitHub into the database.
     * @throws IssueInjectionException if an error occurs during the injection process
     */
    public void injectIssues() throws IssueInjectionException {
        try {
            log.info("Start injecting GitHub issues");

            Set<IssueDTO> issueDTOS = gitHubClient.getIssues();

            Set<Issue> issues = mapIssueDTOs(issueDTOS);

            Map<String, IssueDTO> issueDTOMap =
                    issueDTOS.stream().collect(Collectors.toMap(IssueDTO::id, Function.identity()));

            Set<Issue> issuesWithMilestones = assignTypesAndMilestonesToIssues(issues, issueDTOMap);

            Set<Issue> savedIssues = saveIssues(issuesWithMilestones);

            assignLabelsToIssues(savedIssues, issueDTOMap);
            assignSubIssuesToIssues(savedIssues, issueDTOMap);
        } catch (Exception e) {
            throw new IssueInjectionException("Error while injecting GitHub issues", e);
        }
    }

    /**
     * Maps a set of IssueDTOs to a set of Issue entities.
     * @param issueDTOs the set of IssueDTOs to map
     * @return a set of Issue entities containing the mapped issues with their priorities, points, and other properties
     */
    private Set<Issue> mapIssueDTOs(Set<IssueDTO> issueDTOs) {
        Map<String, IssuePriority> issuePriorityMap = issuePriorityService.getAllIssuePrioritiesMap();

        return issueDTOs.stream()
                .map(dto -> mapDtoToIssue(dto, issuePriorityMap))
                .collect(Collectors.toSet());
    }

    /**
     * Maps an IssueDTO to an Issue entity, setting the issue priority and points if available.
     * @param dto the IssueDTO to map
     * @param issuePriorityMap a map of issue priority IDs to IssuePriority entities
     * @return an Issue entity containing the mapped issue with its priority and points
     */
    private Issue mapDtoToIssue(IssueDTO dto, Map<String, IssuePriority> issuePriorityMap) {
        Issue issue = mapper.toEntity(dto, Issue.class);

        dto.findPriorityOptionId().map(issuePriorityMap::get).ifPresent(issue::setIssuePriority);
        dto.findPoints().ifPresent(issue::setPoints);

        return issue;
    }

    /**
     * Assigns milestones and issue types to issues based on the provided issue DTOs.
     * @param issues the set of issues to assign milestones and issue types to
     * @param issueDtoMap a map of issue IDs to their corresponding issue DTOs
     * @return a set of issues with assigned milestones and issue types
     */
    private Set<Issue> assignTypesAndMilestonesToIssues(Set<Issue> issues, Map<String, IssueDTO> issueDtoMap) {
        Map<String, Milestone> milestoneMap = milestoneService.getAllMilestonesMap();
        Map<String, IssueType> issueTypeMap = issueTypeService.getAllIssueTypesMap();

        issues.forEach(issue -> {
            IssueDTO issueDTO = issueDtoMap.get(issue.getId());
            if (issueDTO != null) {
                if (issueDTO.hasMilestone()) {
                    Milestone milestone = milestoneMap.get(issueDTO.milestone().id());
                    issue.setMilestone(milestone);
                }
                if (issueDTO.hasIssueType()) {
                    IssueType issueType = issueTypeMap.get(issueDTO.issueType().id());
                    issue.setIssueType(issueType);
                }
            }
        });

        return issues;
    }

    /**
     * Assigns sub-issues to issues based on the provided issue DTOs.
     * @param issues the set of issues to assign sub-issues to
     * @param issueDTOMap a map of issue IDs to their corresponding issue DTOs
     */
    private void assignSubIssuesToIssues(Set<Issue> issues, Map<String, IssueDTO> issueDTOMap) {
        Map<String, Issue> issueMap = issues.stream().collect(Collectors.toMap(Issue::getId, Function.identity()));

        for (Issue issue : issues) {
            IssueDTO issueDTO = issueDTOMap.get(issue.getId());
            if (issueDTO == null || !issueDTO.hasSubIssues()) continue;

            Set<Issue> subIssues = issueDTO.subIssues().edges().stream()
                    .filter(Objects::nonNull)
                    .map(GitHubNodeDTO::node)
                    .map(node -> issueMap.get(node.id()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            issue.setSubIssues(subIssues);
        }

        saveIssues(issues);
    }

    /**
     * Assigns labels to issues based on the provided issue DTOs.
     * @param savedIssues the set of saved issues to assign labels to
     * @param issueDtoMap a map of issue IDs to their corresponding issue DTOs
     */
    private void assignLabelsToIssues(Set<Issue> savedIssues, Map<String, IssueDTO> issueDtoMap) {
        Set<IssueLabel> allPullRequestLabels = buildAllIssueLabels(savedIssues, issueDtoMap);

        if (!allPullRequestLabels.isEmpty()) {
            issueLabelRepository.saveAll(allPullRequestLabels);
        }
    }

    /**
     * Builds a set of IssueLabel objects for all issues based on their labels.
     * @param issues the set of issues for which to build labels
     * @param issueDTOMap a map of issue IDs to their corresponding IssueDTOs
     * @return a set of IssueLabel objects representing the labels for all issues
     */
    private Set<IssueLabel> buildAllIssueLabels(Set<Issue> issues, Map<String, IssueDTO> issueDTOMap) {
        Map<String, Label> labelMap = labelService.getAllLabelsMap();
        return issues.stream()
                .map(issue -> getLabelsForIssue(issue, issueDTOMap, labelMap))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    /**
     * Retrieves the labels for a specific issue.
     * @param issue the issue for which to retrieve labels
     * @param issueDTOMap a map of issue IDs to their corresponding IssueDTOs
     * @param labelMap a map of label IDs to their corresponding Label objects
     * @return a list of IssueLabel objects representing the labels for the issue
     */
    private List<IssueLabel> getLabelsForIssue(
            Issue issue, Map<String, IssueDTO> issueDTOMap, Map<String, Label> labelMap) {
        IssueDTO dto = issueDTOMap.get(issue.getId());

        if (dto.hasLabels()) {
            return dto.labels().edges().stream()
                    .map(labelDTO -> new IssueLabel(
                            issue, labelMap.getOrDefault(labelDTO.node().id(), null)))
                    .filter(prLabel -> prLabel.getLabel() != null)
                    .toList();
        }

        return Collections.emptyList();
    }

    /**
     * Saves the provided issues to the database.
     * @param issues the set of issues to save
     * @return a set of saved issues
     */
    private Set<Issue> saveIssues(Set<Issue> issues) {
        List<Issue> savedIssues = issueRepository.saveAll(issues);
        log.info("Successfully saved {} issues", savedIssues.size());
        return new HashSet<>(savedIssues);
    }

    /**
     * Fetches all issues associated with a specific release ID.
     * @param releaseId the ID of the release to fetch issues for
     * @return Set of issues associated with the release, including sub-issues and labels
     */
    public Set<IssueResponse> getIssuesByReleaseId(String releaseId) throws ReleaseNotFoundException {
        Release release = releaseService.checkIfReleaseExists(releaseId);
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
        Milestone milestone = milestoneService.checkIfMilestoneExists(milestoneId);
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
                .filter(l -> labelService.isLabelIncluded(l.getLabel()))
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

    /**
     * Get all issues from the database
     * @return a map of issue id to issue
     */
    public Map<String, Issue> getAllIssuesMap() {
        return issueRepository.findAll().stream().collect(Collectors.toMap(Issue::getId, Function.identity()));
    }
}
