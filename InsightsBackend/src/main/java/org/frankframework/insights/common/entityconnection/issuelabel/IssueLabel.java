package org.frankframework.insights.common.entityconnection.issuelabel;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.frankframework.insights.issue.Issue;
import org.frankframework.insights.label.Label;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
public class IssueLabel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Issue issue;

    @ManyToOne(cascade = {CascadeType.MERGE})
    @JoinColumn(nullable = false)
    private Label label;

    public IssueLabel(Issue issue, Label label) {
        this.issue = issue;
        this.label = label;
    }
}
