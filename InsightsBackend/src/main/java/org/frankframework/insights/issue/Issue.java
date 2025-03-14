package org.frankframework.insights.issue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.Set;
import lombok.Getter;
import org.frankframework.insights.label.Label;
import org.frankframework.insights.milestone.Milestone;
import org.frankframework.insights.pullrequest.PullRequest;

@Entity
@Table(name = "issue")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class Issue {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private int number;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String url;

    @ManyToOne
    private Milestone milestone;

    @ManyToMany
    @JoinTable(
            name = "issue_label",
            joinColumns = @JoinColumn(name = "issue_id"),
            inverseJoinColumns = @JoinColumn(name = "label_id"))
    private Set<Label> labels;

    @OneToMany(mappedBy = "issues")
    private Set<PullRequest> pullRequests;
}
