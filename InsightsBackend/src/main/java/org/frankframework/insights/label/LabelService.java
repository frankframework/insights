package org.frankframework.insights.label;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.configuration.properties.GitHubProperties;
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
    private final List<String> priorityLabels;
	private final List<String> ignoredLabels;

	private static final int MAX_HIGHLIGHTED_LABELS = 15;

    public LabelService(
            GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
            GitHubClient gitHubClient,
            Mapper mapper,
            LabelRepository labelRepository,
            ReleaseIssueHelperService releaseIssueHelperService,
            IssueLabelRepository issueLabelRepository,
            GitHubProperties gitHubProperties) {
        this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.labelRepository = labelRepository;
        this.releaseIssueHelperService = releaseIssueHelperService;
        this.issueLabelRepository = issueLabelRepository;
        this.priorityLabels = gitHubProperties.getPriorityLabels();
		this.ignoredLabels = gitHubProperties.getIgnoredLabels();
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
	public Set<LabelResponse> getHighlightsByReleaseId(String releaseId) throws ReleaseNotFoundException, MappingException {
		List<Label> releaseLabels = getLabelsByReleaseId(releaseId);
		Map<String, Long> labelCountMap = countLabelOccurrences(releaseLabels);
		Map<String, Label> uniqueLabelMap = mapUniqueLabels(releaseLabels);

		List<Label> priorityLabelsList = getSortedLabelsByPriorityAndCount(uniqueLabelMap, labelCountMap);
		List<Label> nonPriorityLabelsList = getSortedLabelsByPriorityAndCount(uniqueLabelMap, labelCountMap);

		List<Label> highlightLabels = selectTopLabels(priorityLabelsList, nonPriorityLabelsList);

		return mapper.toDTO(new LinkedHashSet<>(highlightLabels), LabelResponse.class);
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
	 * Counts occurrences of each label in the provided list.
	 * @param labels the list of labels to count
	 * @return a map of label IDs to their occurrence counts
	 */
	private Map<String, Long> countLabelOccurrences(List<Label> labels) {
		return labels.stream()
				.collect(Collectors.groupingBy(Label::getId, Collectors.counting()));
	}

	/**
	 * Maps unique labels from the provided list to a map.
	 * @param labels the list of labels to map
	 * @return a map of label IDs to Label objects, ensuring uniqueness
	 */
	private Map<String, Label> mapUniqueLabels(List<Label> labels) {
		return labels.stream()
				.collect(Collectors.toMap(Label::getId, Function.identity(), (l1, _) -> l1));
	}

	/**
	 * Filters and sorts labels based on priority and count.
	 * @param uniqueLabelMap the map of unique labels
	 * @param labelCountMap the map of label counts
	 * @return a sorted list of labels that are either priority labels or not ignored
	 */
	private List<Label> getSortedLabelsByPriorityAndCount(
			Map<String, Label> uniqueLabelMap,
			Map<String, Long> labelCountMap) {
		return uniqueLabelMap.values().stream()
				.filter(label -> priorityLabels.contains(label.getColor()))
				.filter(label -> !ignoredLabels.contains(label.getColor()))
				.sorted((l1, l2) -> Long.compare(labelCountMap.getOrDefault(l2.getId(), 0L),
						labelCountMap.getOrDefault(l1.getId(), 0L)))
				.collect(Collectors.toList());
	}

	/**
	 * Selects the top labels from both primary and secondary lists,
	 * @param primaryList the list of primary labels (e.g., priority labels)
	 * @param secondaryList the list of secondary labels (e.g., non-priority labels)
	 * @return a list of selected labels, limited to a maximum number
	 */
	private List<Label> selectTopLabels(List<Label> primaryList, List<Label> secondaryList) {
		return Stream.concat(primaryList.stream(), secondaryList.stream())
				.collect(Collectors.toCollection(LinkedHashSet::new))
				.stream()
				.limit(MAX_HIGHLIGHTED_LABELS)
				.collect(Collectors.toList());
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
