package org.frankframework.insights.common.entityconnection;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.frankframework.insights.issue.Issue;
import org.frankframework.insights.pullrequest.PullRequest;

@Entity
@Getter
@Setter
public class PullRequestIssue {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(nullable = false)
	@JsonIgnore
    private PullRequest pullRequest;

	@ManyToOne(cascade = {CascadeType.MERGE })
	@JoinColumn(nullable = false)
    private Issue issue;
}
