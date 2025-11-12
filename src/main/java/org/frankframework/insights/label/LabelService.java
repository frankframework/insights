package org.frankframework.insights.label;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabel;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabelRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.mapper.MappingException;
import org.frankframework.insights.common.properties.GitHubProperties;
import org.frankframework.insights.github.graphql.GitHubGraphQLClient;
import org.frankframework.insights.github.graphql.GitHubRepositoryStatisticsService;
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
    private final GitHubGraphQLClient gitHubGraphQLClient;
    private final Mapper mapper;
    private final LabelRepository labelRepository;
    private final IssueLabelRepository issueLabelRepository;
    private final ReleaseService releaseService;
    private final List<String> includedLabels;

    private static final int MAX_HIGHLIGHTED_LABELS = 15;

    public LabelService(
            GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
            GitHubGraphQLClient gitHubGraphQLClient,
            Mapper mapper,
            LabelRepository labelRepository,
            IssueLabelRepository issueLabelRepository,
            GitHubProperties gitHubProperties,
            ReleaseService releaseService) {
        this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
        this.gitHubGraphQLClient = gitHubGraphQLClient;
        this.mapper = mapper;
        this.labelRepository = labelRepository;
        this.issueLabelRepository = issueLabelRepository;
        this.releaseService = releaseService;
        this.includedLabels = gitHubProperties.getGraphql().getIncludedLabels();
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
            Set<LabelDTO> labelDTOs = gitHubGraphQLClient.getLabels();
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

        List<Label> highlightLabels = selectFinalHighlights(releaseLabels);

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
     * Selects the top highlighted labels based on their occurrence and inclusion criteria.
     * @param allLabels A list of all labels from a release, including duplicates.
     * @return A list of the top highlighted labels, limited to MAX_HIGHLIGHTED_LABELS.
     */
    private List<Label> selectFinalHighlights(List<Label> allLabels) {
        return calculateLabelCounts(allLabels).entrySet().stream()
                .filter(entry ->
                        includedLabels.contains(entry.getKey().getColor().toUpperCase()))
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .limit(MAX_HIGHLIGHTED_LABELS)
                .toList();
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

    /**
     * Checks if a label is included based on its color.
     * @param label the label to check
     * @return true if the label's color is in the included labels list, false otherwise
     */
    public boolean isLabelIncluded(Label label) {
        return label != null && includedLabels.contains(label.getColor().toUpperCase());
    }
}
