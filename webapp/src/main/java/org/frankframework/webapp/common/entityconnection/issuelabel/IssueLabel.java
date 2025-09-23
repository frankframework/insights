package org.frankframework.webapp.common.entityconnection.issuelabel;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.frankframework.webapp.issue.Issue;
import org.frankframework.webapp.label.Label;

@Entity
@Getter
@Setter
@NoArgsConstructor
@IdClass(IssueLabelId.class)
public class IssueLabel {
    @Id
    @ManyToOne
    @JoinColumn(nullable = false)
    private Issue issue;

    @Id
    @ManyToOne(cascade = {CascadeType.MERGE})
    @JoinColumn(nullable = false)
    private Label label;

    public IssueLabel(Issue issue, Label label) {
        this.issue = issue;
        this.label = label;
    }
}
