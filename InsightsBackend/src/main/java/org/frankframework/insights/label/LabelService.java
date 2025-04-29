package org.frankframework.insights.label;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabel;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabelRepository;
import org.frankframework.insights.common.helper.ReleaseIssueHelperService;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.mapper.MappingException;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.frankframework.insights.issue.Issue;
import org.frankframework.insights.release.ReleaseNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LabelService {

    private final GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;

    private final GitHubClient gitHubClient;

    private final Mapper mapper;

    private final LabelRepository labelRepository;
    private final ReleaseIssueHelperService releaseIssueHelperService;
    private final IssueLabelRepository issueLabelRepository;

    public LabelService(
            GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
            GitHubClient gitHubClient,
            Mapper mapper,
            LabelRepository labelRepository,
            ReleaseIssueHelperService releaseIssueHelperService,
            IssueLabelRepository issueLabelRepository) {
        this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.labelRepository = labelRepository;
        this.releaseIssueHelperService = releaseIssueHelperService;
        this.issueLabelRepository = issueLabelRepository;
    }

    public void injectLabels() throws LabelInjectionException {
        if (gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubLabelCount()
                == labelRepository.count()) {
            log.info("Labels already found in the in database");
            return;
        }

        try {
            log.info("Amount of labels found in database: {}", labelRepository.count());
            log.info(
                    "Amount of labels found in GitHub: {}",
                    gitHubRepositoryStatisticsService
                            .getGitHubRepositoryStatisticsDTO()
                            .getGitHubLabelCount());

            log.info("Start injecting GitHub labels");
            Set<LabelDTO> labelDTOs = gitHubClient.getLabels();
            Set<Label> labels = mapper.toEntity(labelDTOs, Label.class);
            saveLabels(labels);
        } catch (Exception e) {
            throw new LabelInjectionException("Error while injecting GitHub labels", e);
        }
    }

    public Set<LabelResponse> getHighlightsByReleaseId(String releaseId)
            throws ReleaseNotFoundException, MappingException {
        List<Label> releaseLabels = getLabelsByReleaseId(releaseId);
        Map<Label, Long> labelCounts = countLabelOccurrences(releaseLabels);

        Set<Label> filteredLabels = filterLabelsByPercentage(labelCounts);

        return mapper.toDTO(filteredLabels, LabelResponse.class);
    }

    private List<Label> getLabelsByReleaseId(String releaseId) throws ReleaseNotFoundException {
        Set<Issue> releaseIssues = releaseIssueHelperService.getIssuesByReleaseId(releaseId);

        return releaseIssues.stream()
                .flatMap(issue -> issueLabelRepository.findAllByIssue_Id(issue.getId()).stream()
                        .map(IssueLabel::getLabel))
                .collect(Collectors.toList());
    }

    private Map<Label, Long> countLabelOccurrences(List<Label> labels) {
        return labels.stream().collect(Collectors.groupingBy(label -> label, Collectors.counting()));
    }

    private Set<Label> filterLabelsByPercentage(Map<Label, Long> labelCounts) {
        long total = labelCounts.values().stream().mapToLong(Long::longValue).sum();

        double threshold = total * 0.05;

        return labelCounts.entrySet().stream()
                .filter(entry -> entry.getValue() >= threshold)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private void saveLabels(Set<Label> labels) {
        List<Label> savedLabels = labelRepository.saveAll(labels);
        log.info("Successfully saved {} labels", savedLabels.size());
    }
}
