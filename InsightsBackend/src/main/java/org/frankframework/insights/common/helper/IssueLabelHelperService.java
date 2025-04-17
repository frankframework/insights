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

    public void saveIssueLabels(Set<Issue> issues, Map<String, IssueDTO> issueDtoMap) {
        Map<String, Label> labelMap = getAllLabelsMap();

        issues.forEach(issue -> {
            IssueDTO issueDTO = issueDtoMap.get(issue.getId());
            if (issueDTO != null
                    && issueDTO.labels() != null
                    && issueDTO.labels().getEdges() != null) {
                Set<IssueLabel> issueLabels = issueDTO.labels().getEdges().stream()
                        .map(labelDTO -> new IssueLabel(issue, labelMap.getOrDefault(labelDTO.getNode().id, null)))
                        .filter(issueLabel -> issueLabel.getLabel() != null)
                        .collect(Collectors.toSet());

                issueLabelRepository.saveAll(issueLabels);
            }
        });
    }

    public Map<String, Label> getAllLabelsMap() {
        return labelRepository.findAll().stream().collect(Collectors.toMap(Label::getId, label -> label));
    }
}
