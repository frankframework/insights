package org.frankframework.insights.common.entityconnection.pullrequestlabel;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.frankframework.insights.label.Label;
import org.frankframework.insights.pullrequest.PullRequest;

@Entity
@Getter
@Setter
@NoArgsConstructor
@IdClass(PullRequestLabelId.class)
public class PullRequestLabel {
    @Id
    @ManyToOne
    @JoinColumn(nullable = false)
    private PullRequest pullRequest;

    @Id
    @ManyToOne(cascade = {CascadeType.MERGE})
    @JoinColumn(nullable = false)
    private Label label;

    public PullRequestLabel(PullRequest pullRequest, Label label) {
        this.pullRequest = pullRequest;
        this.label = label;
    }
}
