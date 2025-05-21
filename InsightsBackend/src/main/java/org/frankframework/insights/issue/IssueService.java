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
import org.frankframework.insights.label.Label;
import org.frankframework.insights.label.LabelResponse;
import org.frankframework.insights.label.LabelService;
import org.frankframework.insights.milestone.Milestone;
import org.frankframework.insights.milestone.MilestoneNotFoundException;
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
    private final LabelService labelService;
    private final ReleaseIssueHelperService releaseIssueHelperService;

    public IssueService(
            GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
            GitHubClient gitHubClient,
            Mapper mapper,
            IssueRepository issueRepository,
			IssueLabelRepository issueLabelRepository,
            MilestoneService milestoneService,
            LabelService labelService,
            ReleaseIssueHelperService releaseIssueHelperService) {
        this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.issueRepository = issueRepository;
		this.issueLabelRepository = issueLabelRepository;
        this.milestoneService = milestoneService;
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

            Set<Issue> issuesWithMilestones = assignMilestonesToIssues(issues, issueDTOMap);

			Set<Issue> savedIssues = saveIssues(issuesWithMilestones);

			assignLabelsToIssues(savedIssues, issueDTOMap);
			assignSubIssuesToIssues(savedIssues, issueDTOMap);
		} catch (Exception e) {
            throw new IssueInjectionException("Error while injecting GitHub issues", e);
        }
    }

    /**
     * Assigns milestones to issues based on the provided issue DTOs.
     * @param issues the set of issues to assign milestones to
     * @param issueDtoMap a map of issue IDs to their corresponding issue DTOs
     * @return a set of issues with assigned milestones
     */
    private Set<Issue> assignMilestonesToIssues(Set<Issue> issues, Map<String, IssueDTO> issueDtoMap) {
        Map<String, Milestone> milestoneMap = milestoneService.getAllMilestonesMap();

        issues.forEach(issue -> {
            IssueDTO issueDTO = issueDtoMap.get(issue.getId());
            if (issueDTO != null) {
                if (issueDTO.milestone() != null && issueDTO.milestone().id() != null) {
                    Milestone milestone = milestoneMap.get(issueDTO.milestone().id());
                    issue.setMilestone(milestone);
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
		Map<String, Issue> issueMap = issues.stream()
				.collect(Collectors.toMap(Issue::getId, Function.identity()));

		for (Issue issue : issues) {
			IssueDTO issueDTO = issueDTOMap.get(issue.getId());
			if (issueDTO == null || !issueDTO.hasSubIssues()) continue;

			Set<Issue> subIssues = issueDTO.subIssues().getEdges().stream()
					.filter(Objects::nonNull)
					.map(GitHubNodeDTO::getNode)
					.filter(Objects::nonNull)
					.map(node -> issueMap.get(node.id()))
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());

			subIssues.forEach(sub -> sub.setParentIssue(issue));
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
		Map<String, Label> labelMap = labelService.getAllLabelsMap();

		Set<IssueLabel> allPullRequestLabels =
				buildAllIssueLabels(savedIssues, issueDtoMap, labelMap);

		if (!allPullRequestLabels.isEmpty()) {
			issueLabelRepository.saveAll(allPullRequestLabels);
		}
    }

	/**
	 * Builds a set of all issue labels based on the provided issues and their corresponding DTOs.
	 * @param issues the set of issues to process
	 * @param issueDTOMap a map of issue IDs to their corresponding issue DTOs
	 * @param labelMap a map of label IDs to their corresponding labels
	 * @return a set of all issue labels
	 */
	private Set<IssueLabel> buildAllIssueLabels(
			Set<Issue> issues, Map<String, IssueDTO> issueDTOMap, Map<String, Label> labelMap) {
		Set<IssueLabel> allLabels = new HashSet<>();
		for (Issue issue : issues) {
			IssueDTO dto = issueDTOMap.get(issue.getId());
			if (dto != null && dto.hasLabels()) {
				dto.labels().getEdges().stream()
						.map(labelDTO -> new IssueLabel(issue, labelMap.getOrDefault(labelDTO.getNode().id, null)))
						.filter(prLabel -> prLabel.getLabel() != null)
						.forEach(allLabels::add);
			}
		}
		return allLabels;
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
        return mapIssuesToResponsesWithLabels(issues);
    }

    /**
     * Retrieves all issues from the database that are closed.
     * @param releaseId the release ID
     * @return a set of issue responses
     * @throws ReleaseNotFoundException if the release is not found
     */
    public Set<IssueResponse> getIssuesByReleaseId(String releaseId) throws ReleaseNotFoundException {
        Set<Issue> issues = releaseIssueHelperService.getIssuesByReleaseId(releaseId);
        return mapIssuesToResponsesWithLabels(issues);
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
        return mapIssuesToResponsesWithLabels(issues);
    }

    /**
     * Maps a set of issues to a set of issue responses with labels.
     * @param issues the set of issues to map
     * @return a set of issue responses with labels
     */
    private Set<IssueResponse> mapIssuesToResponsesWithLabels(Set<Issue> issues) {
        return issues.stream().map(this::mapIssueWithLabels).collect(Collectors.toSet());
    }

    /**
     * Maps an issue to an issue response and retrieves its labels.
     * @param issue the issue to map2
     * @return the issue response with labels
     */
    private IssueResponse mapIssueWithLabels(Issue issue) {
        IssueResponse issueResponse = mapper.toDTO(issue, IssueResponse.class);
        Set<Label> labels = labelService.getLabelsByIssueId(issue.getId());

        Set<LabelResponse> labelResponses = labels.stream()
                .map(label -> mapper.toDTO(label, LabelResponse.class))
                .collect(Collectors.toSet());
        issueResponse.setLabels(labelResponses);
        return issueResponse;
    }

    /**
     * Get all issues from the database
     * @return a map of issue id to issue
     */
    public Map<String, Issue> getAllIssuesMap() {
        return issueRepository.findAll().stream().collect(Collectors.toMap(Issue::getId, issue -> issue));
    }
}
