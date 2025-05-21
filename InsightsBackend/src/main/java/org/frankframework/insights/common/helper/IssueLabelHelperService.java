package org.frankframework.insights.common.helper;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabel;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabelRepository;
import org.frankframework.insights.issue.Issue;
import org.frankframework.insights.issue.IssueDTO;
import org.frankframework.insights.label.Label;
import org.frankframework.insights.label.LabelRepository;
import org.springframework.stereotype.Service;

@Service
public class IssueLabelHelperService {
    private final IssueLabelRepository issueLabelRepository;
    private final LabelRepository labelRepository;

    public IssueLabelHelperService(IssueLabelRepository issueLabelRepository, LabelRepository labelRepository) {
        this.issueLabelRepository = issueLabelRepository;
        this.labelRepository = labelRepository;
    }

	/**
	 * Saves issue labels to the database.
	 * @param issues Set of issues to save labels for
	 * @param issueDtoMap Map of issue IDs to IssueDTO objects
	 */
    public void saveIssueLabels(Set<Issue> issues, Map<String, IssueDTO> issueDtoMap) {
        Map<String, Label> labelMap = getAllLabelsMap();

        issues.forEach(issue -> handleSaveIssueLabels(issue, issueDtoMap.get(issue.getId()), labelMap));
    }

	/**
	 * Handles the saving of issue labels for a single issue.
	 * @param issue Issue object to save labels for
	 * @param dto IssueDTO object containing label information
	 * @param labelMap Map of label IDs to Label objects
	 */
    private void handleSaveIssueLabels(Issue issue, IssueDTO dto, Map<String, Label> labelMap) {
        if (dto == null || !dto.hasLabels()) return;

        Set<IssueLabel> issueLabels = dto.labels().getEdges().stream()
                .map(labelDTO -> new IssueLabel(issue, labelMap.get(labelDTO.getNode().id)))
                .filter(issueLabel -> issueLabel.getLabel() != null)
                .collect(Collectors.toSet());

        issueLabelRepository.saveAll(issueLabels);
    }

	/**
	 * Fetches all labels from the database and returns them as a map.
	 * @return Map of label IDs to Label objects
	 */
    public Map<String, Label> getAllLabelsMap() {
        return labelRepository.findAll().stream().collect(Collectors.toMap(Label::getId, label -> label));
    }
}
