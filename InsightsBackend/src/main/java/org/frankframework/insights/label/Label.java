package org.frankframework.insights.label;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.Set;
import lombok.Getter;
import org.frankframework.insights.common.entityconnection.IssueLabel;
import org.frankframework.insights.common.entityconnection.PullRequestLabel;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class Label {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(nullable = false)
    private String color;

    @OneToMany
    private Set<IssueLabel> issueLabels;

    @OneToMany
    private Set<PullRequestLabel> pullRequestLabels;
}
