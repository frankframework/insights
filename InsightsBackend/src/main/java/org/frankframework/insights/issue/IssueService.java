package org.frankframework.insights.issue;

import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabel;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabelRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.frankframework.insights.label.Label;
import org.frankframework.insights.label.LabelService;
import org.frankframework.insights.milestone.Milestone;
import org.frankframework.insights.milestone.MilestoneService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IssueService {

    private final GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;
    private final GitHubClient gitHubClient;
    private final Mapper mapper;
    private final IssueRepository issueRepository;
	private final IssueLabelRepository issueLabelRepository;
    private final LabelService labelService;
    private final MilestoneService milestoneService;

    public IssueService(
            GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
            GitHubClient gitHubClient,
            Mapper mapper,
            IssueRepository issueRepository,
			IssueLabelRepository issueLabelRepository,
            LabelService labelService,
            MilestoneService milestoneService) {
        this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.issueRepository = issueRepository;
		this.issueLabelRepository = issueLabelRepository;
        this.labelService = labelService;
        this.milestoneService = milestoneService;
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

            Set<Issue> issuesWithLabelsAndMilestones = assignLabelsAndMilestonesToIssues(issues, issueDtoMap);

            List<Issue> savedIssuesWithLabelsAndMilestones = saveIssues(issuesWithLabelsAndMilestones);

            Set<Issue> issuesWithSubIssues = assignSubIssuesToIssues(savedIssuesWithLabelsAndMilestones, issueDtoMap);

            saveIssues(issuesWithSubIssues);
        } catch (Exception e) {
            throw new IssueInjectionException("Error while injecting GitHub issues", e);
        }
    }

    private Set<Issue> assignLabelsAndMilestonesToIssues(Set<Issue> issues, Map<String, IssueDTO> issueDtoMap) {
        Map<String, Label> labelMap = labelService.getAllLabelsMap();
        Map<String, Milestone> milestoneMap = milestoneService.getAllMilestonesMap();

        issues.forEach(issue -> {
            IssueDTO issueDTO = issueDtoMap.get(issue.getId());
            if (issueDTO != null) {
                Set<IssueLabel> issueLabels = issueDTO.labels().getEdges().stream()
                        .map(labelDTO -> new IssueLabel(issue, labelMap.getOrDefault(labelDTO.getNode().id, null)))
                        .filter(issueLabel -> issueLabel.getLabel() != null)
                        .collect(Collectors.toSet());

                issueLabelRepository.saveAll(issueLabels);

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

        issues.forEach(issue -> {
            IssueDTO issueDTO = issueDtoMap.get(issue.getId());
            if (issueDTO != null
                    && issueDTO.subIssues() != null
                    && issueDTO.subIssues().getEdges() != null) {
                Set<Issue> subIssues = issueDTO.subIssues().getEdges().stream()
                        .map(edge -> issueMap.get(edge.getNode().id()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                subIssues.forEach(sub -> sub.setParentIssue(issue));
                issue.setSubIssues(subIssues);
            }
        });

        return new HashSet<>(issues);
    }

    private List<Issue> saveIssues(Set<Issue> issues) {
        List<Issue> savedIssues = issueRepository.saveAll(issues);
        log.info("Successfully saved {} issues", savedIssues.size());
        return savedIssues;
    }

    public Map<String, Issue> getAllIssuesMap() {
        return issueRepository.findAll().stream().collect(Collectors.toMap(Issue::getId, issue -> issue));
    }
}
