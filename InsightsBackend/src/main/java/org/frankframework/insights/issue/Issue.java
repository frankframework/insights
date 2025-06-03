package org.frankframework.insights.issue;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.frankframework.insights.github.GitHubPropertyState;
import org.frankframework.insights.issuePriority.IssuePriority;
import org.frankframework.insights.issuetype.IssueType;
import org.frankframework.insights.milestone.Milestone;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Issue {
    @Id
    private String id;

    @Column(nullable = false)
    private int number;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private GitHubPropertyState state;

    private OffsetDateTime closedAt;

    @Column(nullable = false)
    private String url;

    @Lob
    private String businessValue;

    @ManyToOne(cascade = CascadeType.MERGE)
    @JsonBackReference("milestone-issue")
    private Milestone milestone;

    @ManyToOne(cascade = CascadeType.MERGE)
    @JsonBackReference("issueType-issue")
    private IssueType issueType;

	@ManyToOne(cascade = CascadeType.MERGE)
	@JsonBackReference("issuePriority-issue")
	private IssuePriority issuePriority;

	private double points;

    @ManyToOne
    @JsonIgnore
    private Issue parentIssue;
}
