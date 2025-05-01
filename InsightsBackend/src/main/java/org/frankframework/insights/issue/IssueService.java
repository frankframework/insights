package org.frankframework.insights.issue;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.helper.IssueLabelHelperService;
import org.frankframework.insights.common.helper.ReleaseIssueHelperService;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.frankframework.insights.label.Label;
import org.frankframework.insights.label.LabelResponse;
import org.frankframework.insights.label.LabelService;
import org.frankframework.insights.milestone.Milestone;
import org.frankframework.insights.milestone.MilestoneNotFoundException;
import org.frankframework.insights.milestone.MilestoneService;
import org.frankframework.insights.release.ReleaseNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IssueService {

    private final GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;
    private final GitHubClient gitHubClient;
    private final Mapper mapper;
    private final IssueRepository issueRepository;
    private final IssueLabelHelperService issueLabelHelperService;
    private final MilestoneService milestoneService;
	private final LabelService labelService;
    private final ReleaseIssueHelperService releaseIssueHelperService;

    public IssueService(
            GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
            GitHubClient gitHubClient,
            Mapper mapper,
            IssueRepository issueRepository,
            IssueLabelHelperService issueLabelHelperService,
            MilestoneService milestoneService,
			LabelService labelService,
            ReleaseIssueHelperService releaseIssueHelperService) {
        this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.issueRepository = issueRepository;
        this.issueLabelHelperService = issueLabelHelperService;
        this.milestoneService = milestoneService;
		this.labelService = labelService;
        this.releaseIssueHelperService = releaseIssueHelperService;
    }

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

            Map<String, IssueDTO> issueDtoMap = issueDTOS.stream().collect(Collectors.toMap(IssueDTO::id, dto -> dto));

            Set<Issue> issuesWithLabelsAndMilestones = assignMilestonesToIssues(issues, issueDtoMap);

            List<Issue> savedIssuesWithLabelsAndMilestones = saveIssues(issuesWithLabelsAndMilestones);

            Set<Issue> issuesWithSubIssues = assignSubIssuesToIssues(savedIssuesWithLabelsAndMilestones, issueDtoMap);

            saveIssues(issuesWithSubIssues);
            issueLabelHelperService.saveIssueLabels(issuesWithSubIssues, issueDtoMap);
        } catch (Exception e) {
            throw new IssueInjectionException("Error while injecting GitHub issues", e);
        }
    }

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

    private Set<Issue> assignSubIssuesToIssues(List<Issue> issues, Map<String, IssueDTO> issueDtoMap) {
        Map<String, Issue> issueMap = issues.stream().collect(Collectors.toMap(Issue::getId, dto -> dto));

        return issues.stream()
                .map(issue -> {
                    IssueDTO dto = issueDtoMap.get(issue.getId());
                    return handleSaveSubIssues(issue, dto, issueMap);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Issue handleSaveSubIssues(Issue issue, IssueDTO dto, Map<String, Issue> issueMap) {
        if (dto == null || !dto.hasSubIssues()) return null;

        Set<Issue> subIssues = dto.subIssues().getEdges().stream()
                .map(edge -> issueMap.get(edge.getNode().id()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        subIssues.forEach(sub -> sub.setParentIssue(issue));
        issue.setSubIssues(subIssues);

        return issue;
    }

    private List<Issue> saveIssues(Set<Issue> issues) {
        List<Issue> savedIssues = issueRepository.saveAll(issues);
        log.info("Successfully saved {} issues", savedIssues.size());
        return savedIssues;
    }

    public Set<IssueResponse> getIssuesByTimespan(OffsetDateTime start, OffsetDateTime end) {
        Set<Issue> issues = issueRepository.findAllByClosedAtBetween(start, end);

		return issues.stream()
				.map(this::mapIssueWithLabels)
				.collect(Collectors.toSet());
    }

    public Set<IssueResponse> getIssuesByReleaseId(String releaseId) throws ReleaseNotFoundException {
        Set<Issue> issues = releaseIssueHelperService.getIssuesByReleaseId(releaseId);

		return issues.stream()
				.map(this::mapIssueWithLabels)
				.collect(Collectors.toSet());
    }

    public Set<IssueResponse> getIssuesByMilestoneId(String milestoneId) throws MilestoneNotFoundException {
        Milestone milestone = milestoneService.checkIfMilestoneExists(milestoneId);
        Set<Issue> issues = issueRepository.findAllByMilestone_Id(milestone.getId());

        return issues.stream()
                .map(this::mapIssueWithLabels)
                .collect(Collectors.toSet());
    }

	private IssueResponse mapIssueWithLabels(Issue issue) {
		IssueResponse issueResponse = mapper.toDTO(issue, IssueResponse.class);
		Set<Label> labels = labelService.getLabelsByIssueId(issue.getId());

		Set<LabelResponse> labelResponses = labels.stream()
				.map(label -> mapper.toDTO(label, LabelResponse.class))
				.collect(Collectors.toSet());
		issueResponse.setLabels(labelResponses);
		return issueResponse;
	}

	public Map<String, Issue> getAllIssuesMap() {
        return issueRepository.findAll().stream().collect(Collectors.toMap(Issue::getId, issue -> issue));
    }
}
