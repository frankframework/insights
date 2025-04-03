package org.frankframework.insights.issue;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.frankframework.insights.common.entityconnection.IssueLabel;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.frankframework.insights.label.Label;
import org.frankframework.insights.label.LabelService;

import org.frankframework.insights.milestone.Milestone;

import org.frankframework.insights.milestone.MilestoneService;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class IssueService {

	private final GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;
	private final GitHubClient gitHubClient;
	private final Mapper mapper;
	private final IssueRepository issueRepository;
	private final LabelService labelService;
	private final MilestoneService milestoneService;

	public IssueService(
			GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
			GitHubClient gitHubClient,
			Mapper mapper,
			IssueRepository issueRepository,
			LabelService labelService,
			MilestoneService milestoneService) {
		this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
		this.gitHubClient = gitHubClient;
		this.mapper = mapper;
		this.issueRepository = issueRepository;
		this.labelService = labelService;
		this.milestoneService = milestoneService;
	}

	public void injectIssues() throws IssueInjectionException {
		if (gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubIssueCount()
				== issueRepository.count()) {
			log.info("Issues already found in the in database");
			return;
		}

		try {
			log.info("Amount of issues found in database: {}", issueRepository.count());
			log.info("Amount of issues found in GitHub: {}", gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubIssueCount());

			log.info("Start injecting GitHub issues");
			Set<IssueDTO> issueDTOS = gitHubClient.getIssues();

			Set<Issue> issues = mapper.toEntity(issueDTOS, Issue.class);

			List<Label> labels = labelService.getAllLabels();
			List<Milestone> milestones = milestoneService.getAllMilestones();

			Set<Issue> updatedIssues = assignSubPropertiesToIssues(issues, issueDTOS, labels, milestones);

			saveIssues(updatedIssues);
		} catch (Exception e) {
			throw new IssueInjectionException("Error while injecting GitHub issues", e);
		}
	}

	private Set<Issue> assignSubPropertiesToIssues(Set<Issue> issues, Set<IssueDTO> issueDTOS, List<Label> labels, List<Milestone> milestones) {
		Map<String, Label> labelMap = labels.stream().collect(Collectors.toMap(Label::getId, label -> label));
		Map<String, Milestone> milestoneMap = milestones.stream().collect(Collectors.toMap(Milestone::getId, milestone -> milestone));

		for (Issue issue : issues) {
			issueDTOS.stream()
					.filter(dto -> Objects.equals(dto.id(), issue.getId()))
					.findFirst()
					.ifPresent(issueDTO -> {
						Set<IssueLabel> issueLabels = issueDTO.labels().getEdges().stream()
								.map(labelDTO -> new IssueLabel(issue, labelMap.getOrDefault(labelDTO.getNode().id, null)))
								.filter(issueLabel -> issueLabel.getLabel() != null)
								.collect(Collectors.toSet());
						issue.setIssueLabels(issueLabels);

						if (issueDTO.milestone() != null && issueDTO.milestone().id() != null) {
							Milestone milestone = milestoneMap.get(issueDTO.milestone().id());
							issue.setMilestone(milestone);
						}
					});
		}
		return issues;
	}

	private void saveIssues(Set<Issue> issues) {
		List<Issue> savedIssues = issueRepository.saveAll(issues);
		log.info("Successfully saved {} issues", savedIssues.size());
	}
}
