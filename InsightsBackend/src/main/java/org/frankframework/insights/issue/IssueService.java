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
                if (issueDTO.hasMilestone()) {
                    Milestone milestone = milestoneMap.get(issueDTO.milestone().id());
                    issue.setMilestone(milestone);
                }
                if (issueDTO.hasIssueType()) {
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

            issueDTO.subIssues().getEdges().stream()
                    .filter(Objects::nonNull)
                    .map(GitHubNodeDTO::getNode)
                    .map(node -> issueMap.get(node.id()))
                    .forEach(subIssue -> subIssue.setParentIssue(issue));
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
    private Set<IssueLabel> buildAllIssueLabels(
            Set<Issue> issues, Map<String, IssueDTO> issueDTOMap) {
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
	 * @return a set of IssueResponse objects representing the mapped issues
	 */
	public Set<IssueResponse> mapIssuesToResponses(Set<Issue> issues) {
		Set<Issue> parentIssues = findRootParentIssues(issues);
		return parentIssues.stream()
				.map(this::mapIssueTree)
				.collect(Collectors.toSet());
	}

	/**
	 * Finds root parent issues from a set of issues.
	 * @param issues the set of issues to search for root parents
	 * @return a set of root parent issues that do not have any children in the provided set
	 */
	private Set<Issue> findRootParentIssues(Set<Issue> issues) {
		Set<String> childIds = getChildIssueIds(issues);
		Set<Issue> rootCandidates = getRootCandidates(issues);
		return filterNonDuplicatedRoots(rootCandidates, childIds);
	}

	/**
	 * Collects the IDs of all child issues from a set of issues.
	 * @param issues the set of issues to search for child IDs
	 * @return a set of IDs representing the child issues
	 */
	private Set<String> getChildIssueIds(Set<Issue> issues) {
		return issues.stream()
				.filter(issue -> issue.getParentIssue() != null)
				.map(Issue::getId)
				.collect(Collectors.toSet());
	}

	/**
	 * Filters the provided set of issues to find root candidates, which are issues without a parent.
	 * @param issues the set of issues to filter
	 * @return a set of issues that are root candidates (i.e., they do not have a parent issue)
	 */
	private Set<Issue> getRootCandidates(Set<Issue> issues) {
		return issues.stream()
				.filter(issue -> issue.getParentIssue() == null)
				.collect(Collectors.toSet());
	}

	/**
	 * Filters out issues that are not root issues by checking against a set of child IDs.
	 * @param rootCandidates the set of candidate root issues to filter
	 * @param childIds the set of IDs representing child issues
	 * @return a set of root issues that do not have any children in the provided set
	 */
	private Set<Issue> filterNonDuplicatedRoots(Set<Issue> rootCandidates, Set<String> childIds) {
		return rootCandidates.stream()
				.filter(issue -> !childIds.contains(issue.getId()))
				.collect(Collectors.toSet());
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
