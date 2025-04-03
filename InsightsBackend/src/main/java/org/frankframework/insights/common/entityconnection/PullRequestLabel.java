package org.frankframework.insights.common.entityconnection;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.frankframework.insights.label.Label;
import org.frankframework.insights.pullrequest.PullRequest;

@Entity
@Getter
@Setter
public class PullRequestLabel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private PullRequest pullRequest;

	@ManyToOne(cascade = { CascadeType.MERGE })
    @JoinColumn(nullable = false)
    private Label label;
}
