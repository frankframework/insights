package org.frankframework.insights.label;

import java.util.Collections;
import java.util.Comparator;
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
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.mapper.MappingException;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.frankframework.insights.release.Release;
import org.frankframework.insights.release.ReleaseNotFoundException;
import org.frankframework.insights.release.ReleaseService;
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
    private final IssueLabelRepository issueLabelRepository;
    private final ReleaseService releaseService;
    private final List<String> priorityLabels;
    private final List<String> ignoredLabels;

    private static final int MAX_HIGHLIGHTED_LABELS = 15;

    public LabelService(
            GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
            GitHubClient gitHubClient,
            Mapper mapper,
            LabelRepository labelRepository,
            IssueLabelRepository issueLabelRepository,
            GitHubProperties gitHubProperties,
            ReleaseService releaseService) {
        this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.labelRepository = labelRepository;
        this.issueLabelRepository = issueLabelRepository;
        this.releaseService = releaseService;
        this.priorityLabels = gitHubProperties.getPriorityLabels().stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());
        this.ignoredLabels = gitHubProperties.getIgnoredLabels().stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());
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
     * Fetches and processes labels associated with a specific release to determine highlights.
     * @param releaseId the ID of the release
     * @return a set of highlighted labels for the release
     * @throws ReleaseNotFoundException if the release is not found
     * @throws MappingException if there is an error during mapping
     */
    public Set<LabelResponse> getHighlightsByReleaseId(String releaseId)
            throws ReleaseNotFoundException, MappingException {
        List<Label> releaseLabels = getLabelsForRelease(releaseId);
        if (releaseLabels.isEmpty()) {
            return Collections.emptySet();
        }

        Map<Label, Long> labelCounts = calculateLabelCounts(releaseLabels);

        List<Label> highlightLabels = selectFinalHighlights(labelCounts);

        return mapper.toDTO(new LinkedHashSet<>(highlightLabels), LabelResponse.class);
    }

    /**
     * Fetches all labels associated with a specific release.
     * @param releaseId the ID of the release
     * @return a list of labels associated with the release
     * @throws ReleaseNotFoundException if the release is not found
     */
    private List<Label> getLabelsForRelease(String releaseId) throws ReleaseNotFoundException {
        Release release = releaseService.checkIfReleaseExists(releaseId);
        return labelRepository.findLabelsByReleaseId(release.getId());
    }

    /**
     * Groups labels and counts their occurrences.
     * @param allLabels A list of all labels from a release, including duplicates.
     * @return A Map where the key is the unique Label and the value is its occurrence count.
     */
    private Map<Label, Long> calculateLabelCounts(List<Label> allLabels) {
        return allLabels.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    /**
     * Selects the final highlighted labels based on priority, ignoring certain labels,
     * @param labelCounts A map of labels to their occurrence counts.
     * @return A list of Labels that are highlighted for the release.
     */
    private List<Label> selectFinalHighlights(Map<Label, Long> labelCounts) {
        List<Map.Entry<Label, Long>> sortedCandidates = filterAndSortCandidates(labelCounts);
        return combineAndLimitHighlights(sortedCandidates);
    }

    /**
     * Filters out ignored labels and sorts the remaining candidates by their counts in descending order.
     * @param labelCounts A map of labels to their occurrence counts.
     * @return A sorted list of label entries, excluding ignored labels.
     */
    private List<Map.Entry<Label, Long>> filterAndSortCandidates(Map<Label, Long> labelCounts) {
        return labelCounts.entrySet().stream()
                .filter(entry ->
                        !ignoredLabels.contains(entry.getKey().getColor().toUpperCase()))
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    /**
     * Separates a sorted list of candidates into priority and non-priority groups,
     * then combines them with priority labels first and limits the result.
     * @param sortedCandidates A pre-sorted list of candidate labels.
     * @return The final, ordered, and limited list of Labels.
     */
    private List<Label> combineAndLimitHighlights(List<Map.Entry<Label, Long>> sortedCandidates) {
        List<Label> priorityLabels = extractPriorityLabels(sortedCandidates);
        List<Label> nonPriorityLabels = extractNonPriorityLabels(sortedCandidates);
        return buildFinalHighlightsList(priorityLabels, nonPriorityLabels);
    }

    /**
     * Filters a list of candidates to return only the priority labels.
     * @param candidates The list of sorted candidate entries.
     * @return A list of Labels that are in the priority list.
     */
    private List<Label> extractPriorityLabels(List<Map.Entry<Label, Long>> candidates) {
        return candidates.stream()
                .filter(entry -> isPriorityLabel(entry.getKey()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Filters a list of candidates to return only the non-priority labels.
     * @param candidates The list of sorted candidate entries.
     * @return A list of Labels that are not in the priority list.
     */
    private List<Label> extractNonPriorityLabels(List<Map.Entry<Label, Long>> candidates) {
        return candidates.stream()
                .filter(entry -> !isPriorityLabel(entry.getKey()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Combines the priority and non-priority lists and limits the result to the maximum size.
     * @param priority The list of priority labels.
     * @param nonPriority The list of non-priority labels.
     * @return The final, combined, and limited list of highlight labels.
     */
    private List<Label> buildFinalHighlightsList(List<Label> priority, List<Label> nonPriority) {
        return Stream.concat(priority.stream(), nonPriority.stream())
                .limit(MAX_HIGHLIGHTED_LABELS)
                .collect(Collectors.toList());
    }

    /**
     * Helper predicate to check if a label is a priority label (case-insensitive).
     */
    private boolean isPriorityLabel(Label label) {
        return priorityLabels.contains(label.getColor().toUpperCase());
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
