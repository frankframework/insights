package org.frankframework.insights.issue;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.businessvalue.BusinessValue;
import org.frankframework.insights.common.client.graphql.GraphQLNodeDTO;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabel;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabelRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.graphql.GitHubGraphQLClient;
import org.frankframework.insights.issueprojects.IssuePriority;
import org.frankframework.insights.issueprojects.IssueProjectItemsInjectionService;
import org.frankframework.insights.issueprojects.IssueState;
import org.frankframework.insights.issuetype.IssueType;
import org.frankframework.insights.issuetype.IssueTypeInjectionService;
import org.frankframework.insights.label.Label;
import org.frankframework.insights.label.LabelInjectionService;
import org.frankframework.insights.milestone.Milestone;
import org.frankframework.insights.milestone.MilestoneInjectionService;
import org.springframework.stereotype.Service;

/**
 * Service class for injecting issues.
 * Handles the injection, mapping, and processing of GitHub issues into the database.
 */
@Service
@Slf4j
public class IssueInjectionService {
    private final GitHubGraphQLClient gitHubGraphQLClient;
    private final Mapper mapper;
    private final IssueRepository issueRepository;
    private final IssueLabelRepository issueLabelRepository;
    private final MilestoneInjectionService milestoneInjectionService;
    private final IssueTypeInjectionService issueTypeInjectionService;
    private final LabelInjectionService labelInjectionService;
    private final IssueProjectItemsInjectionService issueProjectItemsInjectionService;

    public IssueInjectionService(
            GitHubGraphQLClient gitHubGraphQLClient,
            Mapper mapper,
            IssueRepository issueRepository,
            IssueLabelRepository issueLabelRepository,
            MilestoneInjectionService milestoneInjectionService,
            IssueTypeInjectionService issueTypeInjectionService,
            LabelInjectionService labelInjectionService,
            IssueProjectItemsInjectionService issueProjectItemsInjectionService) {
        this.gitHubGraphQLClient = gitHubGraphQLClient;
        this.mapper = mapper;
        this.issueRepository = issueRepository;
        this.issueLabelRepository = issueLabelRepository;
        this.milestoneInjectionService = milestoneInjectionService;
        this.issueTypeInjectionService = issueTypeInjectionService;
        this.labelInjectionService = labelInjectionService;
        this.issueProjectItemsInjectionService = issueProjectItemsInjectionService;
    }

    /**
     * Injects issues from GitHub into the database.
     * @throws IssueInjectionException if an error occurs during the injection process
     */
    public void injectIssues() throws IssueInjectionException {
        try {
            log.info("Start injecting GitHub issues");

            Set<IssueDTO> issueDTOS = gitHubGraphQLClient.getIssues();

            Set<Issue> issues = mapIssueDTOs(issueDTOS);

            Map<String, IssueDTO> issueDTOMap =
                    issueDTOS.stream().collect(Collectors.toMap(IssueDTO::id, Function.identity()));

            Set<Issue> issuesWithMilestones = assignTypesAndMilestonesToIssues(issues, issueDTOMap);

            restoreBusinessValueLinks(issuesWithMilestones);

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
        Map<String, IssuePriority> issuePriorityMap = issueProjectItemsInjectionService.getAllIssuePrioritiesMap();
        Map<String, IssueState> issueStateMap = issueProjectItemsInjectionService.getAllIssueStatesMap();

        return issueDTOs.stream()
                .map(dto -> mapDtoToIssue(dto, issuePriorityMap, issueStateMap))
                .collect(Collectors.toSet());
    }

    /**
     * Maps an IssueDTO to an Issue entity, setting the issue priority and points if available.
     * @param dto the IssueDTO to map
     * @param issuePriorityMap a map of issue priority IDs to IssuePriority entities
     * @return an Issue entity containing the mapped issue with its priority and points
     */
    private Issue mapDtoToIssue(
            IssueDTO dto, Map<String, IssuePriority> issuePriorityMap, Map<String, IssueState> issueStateMap) {
        Issue issue = mapper.toEntity(dto, Issue.class);

        dto.findPriorityOptionId().map(issuePriorityMap::get).ifPresent(issue::setIssuePriority);
        dto.findStatusOptionId().map(issueStateMap::get).ifPresent(issue::setIssueState);
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
        Map<String, Milestone> milestoneMap = milestoneInjectionService.getAllMilestonesMap();
        Map<String, IssueType> issueTypeMap = issueTypeInjectionService.getAllIssueTypesMap();

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
                    .map(GraphQLNodeDTO::node)
                    .map(node -> issueMap.get(node.id()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            issue.setSubIssues(subIssues);
        }

        saveIssues(issues);
    }

    /**
     * Assigns labels to issues based on the provided issue DTOs.
     * Deletes all existing label associations for the issues before assigning new ones.
     * @param savedIssues the set of saved issues to assign labels to
     * @param issueDtoMap a map of issue IDs to their corresponding issue DTOs
     */
    private void assignLabelsToIssues(Set<Issue> savedIssues, Map<String, IssueDTO> issueDtoMap) {
        List<String> issueIds = savedIssues.stream().map(Issue::getId).toList();

        if (!issueIds.isEmpty()) {
            issueLabelRepository.deleteAllByIssue_IdIn(issueIds);
        }

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
        Map<String, Label> labelMap = labelInjectionService.getAllLabelsMap();
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
     * Re-applies existing businessValue links to newly mapped issues before saving.
     * This method looks up which issues already have a businessValue in the database
     * and carries those links over to the new fetched issues.
     */
    private void restoreBusinessValueLinks(Set<Issue> issues) {
        Set<String> ids = issues.stream().map(Issue::getId).collect(Collectors.toSet());
        Map<String, BusinessValue> existingLinks = issueRepository.findAllByIdInAndBusinessValueIsNotNull(ids).stream()
                .collect(Collectors.toMap(Issue::getId, Issue::getBusinessValue));

        if (existingLinks.isEmpty()) {
            return;
        }

        issues.stream()
                .filter(issue -> existingLinks.containsKey(issue.getId()))
                .forEach(issue -> issue.setBusinessValue(existingLinks.get(issue.getId())));

        log.info("Restored {} business value link(s) on re-injected issues", existingLinks.size());
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
     * Get all issues from the database.
     * Used while injecting pull requests to resolve their closing issue references.
     * @return a map of issue id to issue
     */
    public Map<String, Issue> getAllIssuesMap() {
        return issueRepository.findAll().stream().collect(Collectors.toMap(Issue::getId, Function.identity()));
    }
}
