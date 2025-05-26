package org.frankframework.insights.label;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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

/**
 * Service class for managing labels.
 * Handles the injection, mapping, and processing of GitHub labels into the database.
 */
@Service
@Slf4j
public class LabelService {
    private final GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;
    private final GitHubClient gitHubClient;
    private final Mapper mapper;
    private final LabelRepository labelRepository;
    private final ReleaseIssueHelperService releaseIssueHelperService;
    private final IssueLabelRepository issueLabelRepository;

    private static final double HIGHLIGHTS_LABEL_THRESHOLD = 0.05;

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

    /**
     * Injects labels from GitHub into the database.
     * @throws LabelInjectionException if an error occurs during the injection process.
     */
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

    /**
     * Fetches labels from the database and returns them as a set of LabelResponse objects.
     * @param releaseId the ID of the release for which to fetch labels
     * @return a set of LabelResponse objects
     * @throws ReleaseNotFoundException if the release is not found
     * @throws MappingException if an error occurs during mapping
     */
    public Set<LabelResponse> getHighlightsByReleaseId(String releaseId)
            throws ReleaseNotFoundException, MappingException {
        List<Label> releaseLabels = getLabelsByReleaseId(releaseId);
        Map<Label, Long> labelCounts = countLabelOccurrences(releaseLabels);

        Set<Label> filteredLabels = filterLabelsByPercentage(labelCounts);

        return mapper.toDTO(filteredLabels, LabelResponse.class);
    }

    /**
     * Fetches labels associated with a specific release ID.
     * @param releaseId the ID of the release
     * @return a list of labels associated with the release
     * @throws ReleaseNotFoundException if the release is not found
     */
    private List<Label> getLabelsByReleaseId(String releaseId) throws ReleaseNotFoundException {
        Set<Issue> releaseIssues = releaseIssueHelperService.getIssuesByReleaseId(releaseId);

        return releaseIssues.stream()
                .flatMap(issue -> issueLabelRepository.findAllByIssue_Id(issue.getId()).stream()
                        .map(IssueLabel::getLabel))
                .collect(Collectors.toList());
    }

    /**
     * Counts the occurrences of each label in the provided list.
     * @param labels the list of labels to count
     * @return a map where the keys are labels and the values are their respective counts
     */
    private Map<Label, Long> countLabelOccurrences(List<Label> labels) {
        return labels.stream().collect(Collectors.groupingBy(label -> label, Collectors.counting()));
    }

    /**
     * Filters labels based on a percentage threshold.
     * @param labelCounts a map of labels and their respective counts
     * @return a set of labels that meet the percentage threshold
     */
    private Set<Label> filterLabelsByPercentage(Map<Label, Long> labelCounts) {
        long total = labelCounts.values().stream().mapToLong(Long::longValue).sum();

        double threshold = total * HIGHLIGHTS_LABEL_THRESHOLD;

        return labelCounts.entrySet().stream()
                .filter(entry -> entry.getValue() >= threshold)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

	/**
	 * Fetches all labels from the database and returns them as a map.
	 * @return Map of label IDs to Label objects
	 */
	public Map<String, Label> getAllLabelsMap() {
		return labelRepository.findAll().stream().collect(Collectors.toMap(Label::getId, Function.identity()));
	}

    /**
     * Fetches labels associated with a specific issue ID.
     * @param issueId the ID of the issue
     * @return a set of labels associated with the issue
     */
    public Set<Label> getLabelsByIssueId(String issueId) {
        return issueLabelRepository.findAllByIssue_Id(issueId).stream()
                .map(IssueLabel::getLabel)
                .collect(Collectors.toSet());
    }

    /**
     * Saves a set of labels to the database.
     * @param labels the set of labels to save
     */
    private void saveLabels(Set<Label> labels) {
        List<Label> savedLabels = labelRepository.saveAll(labels);
        log.info("Successfully saved {} labels", savedLabels.size());
    }
}
