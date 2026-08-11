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
import org.frankframework.insights.release.Release;
import org.frankframework.insights.release.ReleaseNotFoundException;
import org.frankframework.insights.release.ReleaseQueryService;
import org.springframework.stereotype.Service;

/**
 * Service class for reading labels from the database.
 * Filling the label tables is the responsibility of the data import module.
 */
@Service
@Slf4j
public class LabelQueryService {
    private final Mapper mapper;
    private final LabelRepository labelRepository;
    private final IssueLabelRepository issueLabelRepository;
    private final ReleaseQueryService releaseQueryService;
    private final List<String> includedLabels;

    public LabelQueryService(
            Mapper mapper,
            LabelRepository labelRepository,
            IssueLabelRepository issueLabelRepository,
            GitHubProperties gitHubProperties,
            ReleaseQueryService releaseQueryService) {
        this.mapper = mapper;
        this.labelRepository = labelRepository;
        this.issueLabelRepository = issueLabelRepository;
        this.releaseQueryService = releaseQueryService;
        this.includedLabels = gitHubProperties.getGraphql().getIncludedLabels();
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
        Release release = releaseQueryService.checkIfReleaseExists(releaseId);
        return labelRepository.findLabelsByReleaseId(release.getId());
    }

    /**
     * Selects highlighted labels based on their occurrence and inclusion criteria.
     * @param allLabels A list of all labels from a release, including duplicates.
     * @return A list of highlighted labels sorted by occurrence count.
     */
    private List<Label> selectFinalHighlights(List<Label> allLabels) {
        return calculateLabelCounts(allLabels).entrySet().stream()
                .filter(entry ->
                        includedLabels.contains(entry.getKey().getColor().toUpperCase()))
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
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
     * Checks if a label is included based on its color.
     * @param label the label to check
     * @return true if the label's color is in the included labels list, false otherwise
     */
    public boolean isLabelIncluded(Label label) {
        return label != null && includedLabels.contains(label.getColor().toUpperCase());
    }
}
