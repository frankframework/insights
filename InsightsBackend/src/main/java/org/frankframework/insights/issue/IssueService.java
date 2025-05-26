package org.frankframework.insights.issue;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabel;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabelRepository;
import org.frankframework.insights.common.helper.ReleaseIssueHelperService;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubNodeDTO;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
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
import org.frankframework.insights.release.ReleaseNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Service class for managing issues.
 * Handles the injection, mapping, and processing of GitHub issues into the database.
 */
@Service
@Slf4j
public class IssueService {

    private final GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;
    private final GitHubClient gitHubClient;
    private final Mapper mapper;
    private final IssueRepository issueRepository;
    private final IssueLabelRepository issueLabelRepository;
    private final MilestoneService milestoneService;
    private final IssueTypeService issueTypeService;
    private final LabelService labelService;
    private final ReleaseIssueHelperService releaseIssueHelperService;

    public IssueService(
            GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
            GitHubClient gitHubClient,
            Mapper mapper,
            IssueRepository issueRepository,
            IssueLabelRepository issueLabelRepository,
            MilestoneService milestoneService,
            IssueTypeService issueTypeService,
            LabelService labelService,
            ReleaseIssueHelperService releaseIssueHelperService) {
        this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.issueRepository = issueRepository;
        this.issueLabelRepository = issueLabelRepository;
        this.milestoneService = milestoneService;
        this.issueTypeService = issueTypeService;
        this.labelService = labelService;
        this.releaseIssueHelperService = releaseIssueHelperService;
    }

    /**
     * Injects issues from GitHub into the database.
     * @throws IssueInjectionException if an error occurs during the injection process
     */
    public void injectIssues() throws IssueInjectionException {
        if (gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubIssueCount()
                == issueRepository.count()) {
            log.info("Issues already found in the database");
            return;
        }

        try {
            log.info("Amount of issues found in database: {}", issueRepository.count());
            log.info(
                    "Amount of issues found in GitHub: {}",
                    gitHubRepositoryStatisticsService
                            .getGitHubRepositoryStatisticsDTO()
                            .getGitHubIssueCount());
            log.info("Start injecting GitHub issues");

            Set<IssueDTO> issueDTOS = gitHubClient.getIssues();
            Set<Issue> issues = mapper.toEntity(issueDTOS, Issue.class);

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
                if (issueDTO.milestone() != null && issueDTO.milestone().id() != null) {
                    Milestone milestone = milestoneMap.get(issueDTO.milestone().id());
                    issue.setMilestone(milestone);
                }
                if (issueDTO.issueType() != null && issueDTO.issueType().id != null) {
                    IssueType issueType = issueTypeMap.get(issueDTO.issueType().id);
                    issue.setIssueType(issueType);
                }
            }
        });

        return issues;
    }

    /**
     * Assigns sub-issues to their parent issues based on the provided issue DTOs.
     * @param issues the set of issues to assign sub-issues to
     * @param issueDTOMap a map of issue IDs to their corresponding issue DTOs
     */
    private void assignSubIssuesToIssues(Set<Issue> issues, Map<String, IssueDTO> issueDTOMap) {
        Map<String, Issue> issueMap = issues.stream().collect(Collectors.toMap(Issue::getId, Function.identity()));

        for (Issue issue : issues) {
            IssueDTO issueDTO = issueDTOMap.get(issue.getId());
            if (issueDTO == null || !issueDTO.hasSubIssues()) continue;

            Set<Issue> subIssues = issueDTO.subIssues().getEdges().stream()
                    .filter(Objects::nonNull)
                    .map(GitHubNodeDTO::getNode)
                    .map(node -> issueMap.get(node.id()))
                    .collect(Collectors.toSet());

            subIssues.forEach(subIssue -> {
                subIssue.setParentIssue(issue);
            });
        }

        saveIssues(issues);
    }

    /**
     * Assigns labels to issues based on the provided issue DTOs.
     * @param savedIssues the set of saved issues to assign labels to
     * @param issueDtoMap a map of issue IDs to their corresponding issue DTOs
     */
    private void assignLabelsToIssues(Set<Issue> savedIssues, Map<String, IssueDTO> issueDtoMap) {
        Map<String, Label> labelMap = labelService.getAllLabelsMap();

        Set<IssueLabel> allPullRequestLabels = buildAllIssueLabels(savedIssues, issueDtoMap, labelMap);

        if (!allPullRequestLabels.isEmpty()) {
            issueLabelRepository.saveAll(allPullRequestLabels);
        }
    }

    private Set<IssueLabel> buildAllIssueLabels(
            Set<Issue> issues, Map<String, IssueDTO> issueDTOMap, Map<String, Label> labelMap) {
        return issues.stream()
                .map(issue -> getLabelsForIssue(issue, issueDTOMap, labelMap))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private List<IssueLabel> getLabelsForIssue(
            Issue issue, Map<String, IssueDTO> issueDTOMap, Map<String, Label> labelMap) {
        IssueDTO dto = issueDTOMap.get(issue.getId());

        if (dto != null && dto.hasLabels()) {
            return dto.labels().getEdges().stream()
                    .map(labelDTO -> new IssueLabel(issue, labelMap.getOrDefault(labelDTO.getNode().id, null)))
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
     * Retrieves all issues from the database.
     * @param start the start date of the timespan
     * @param end the end date of the timespan
     * @return a set of issue responses
     */
    public Set<IssueResponse> getIssuesByTimespan(OffsetDateTime start, OffsetDateTime end) {
        Set<Issue> issues = issueRepository.findAllByClosedAtBetween(start, end);
        return mapIssuesToResponses(issues);
    }

    /**
     * Retrieves all issues from the database that are closed.
     * @param releaseId the release ID
     * @return a set of issue responses
     * @throws ReleaseNotFoundException if the release is not found
     */
    public Set<IssueResponse> getIssuesByReleaseId(String releaseId) throws ReleaseNotFoundException {
        Set<Issue> issues = releaseIssueHelperService.getIssuesByReleaseId(releaseId);
        return mapIssuesToResponses(issues);
    }

    /**
     * Retrieves all issues from the database that are associated with a specific milestone.
     * @param milestoneId the milestone ID
     * @return a set of issue responses
     * @throws MilestoneNotFoundException if the milestone is not found
     */
    public Set<IssueResponse> getIssuesByMilestoneId(String milestoneId) throws MilestoneNotFoundException {
        Milestone milestone = milestoneService.checkIfMilestoneExists(milestoneId);
        Set<Issue> issues = issueRepository.findAllByMilestone_Id(milestone.getId());
        return mapIssuesToResponses(issues);
    }

    /**
     * Maps a set of issues to a set of IssueResponse objects.
     * @param issues the set of issues to map
     * @return a set of IssueResponse objects containing the mapped issues with their labels, milestone, issue type and subIssues
     */
    private Set<IssueResponse> mapIssuesToResponses(Set<Issue> issues) {
        return issues.stream().map(this::mapIssueTree).collect(Collectors.toSet());
    }

    /**
     * Maps an issue to an IssueResponse object, including its labels, milestone, issue type and subIssues.
     * @param issue the issue to map
     * @return an IssueResponse object containing the mapped issue with its labels, milestone, issue type and subIssues
     */
    private IssueResponse mapIssueTree(Issue issue) {
        IssueResponse response = mapper.toDTO(issue, IssueResponse.class);
        response.setLabels(mapLabelsForIssue(issue));
        response.setMilestone(mapMilestoneForIssue(issue));
        response.setIssueType(mapIssueTypeForIssue(issue));
        response.setSubIssues(mapSubIssuesForIssue(issue));
        return response;
    }

    /**
     * Maps the labels for an issue to a set of LabelResponse objects.
     * @param issue the issue for which to map the labels
     * @return a set of LabelResponse objects representing the labels for the issue
     */
    private Set<LabelResponse> mapLabelsForIssue(Issue issue) {
        return labelService.getLabelsByIssueId(issue.getId()).stream()
                .map(label -> mapper.toDTO(label, LabelResponse.class))
                .collect(Collectors.toSet());
    }

    /**
     * Maps the milestone for an issue, or null if none.
     * @param issue the issue for which to map the milestone
     * @return a MilestoneResponse object representing the milestone, or null if not present
     */
    private MilestoneResponse mapMilestoneForIssue(Issue issue) {
        return Optional.ofNullable(issue.getMilestone())
                .map(milestone -> mapper.toDTO(milestone, MilestoneResponse.class))
                .orElse(null);
    }

    /**
     * Maps the issue type for an issue, or null if none.
     * @param issue the issue for which to map the issue type
     * @return an IssueTypeResponse object representing the issue type, or null if not present
     */
    private IssueTypeResponse mapIssueTypeForIssue(Issue issue) {
        return Optional.ofNullable(issue.getIssueType())
                .map(type -> mapper.toDTO(type, IssueTypeResponse.class))
                .orElse(null);
    }

    /**
     * Maps the sub-issues for an issue, or returns an empty set if none.
     * @param issue the issue for which to map sub-issues
     * @return a set of IssueResponse objects representing the sub-issues
     */
    private Set<IssueResponse> mapSubIssuesForIssue(Issue issue) {
        Set<Issue> subIssues = issueRepository.findAllByParentIssue_Id(issue.getId());
        return subIssues.isEmpty()
                ? Collections.emptySet()
                : subIssues.stream().map(this::mapIssueTree).collect(Collectors.toSet());
    }

    /**
     * Get all issues from the database
     * @return a map of issue id to issue
     */
    public Map<String, Issue> getAllIssuesMap() {
        return issueRepository.findAll().stream().collect(Collectors.toMap(Issue::getId, Function.identity()));
    }
}
