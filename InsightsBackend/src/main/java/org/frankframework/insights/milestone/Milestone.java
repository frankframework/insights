package org.frankframework.insights.milestone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.Set;
import lombok.Getter;
import org.frankframework.insights.github.GitHubPropertyState;
import org.frankframework.insights.issue.Issue;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class Milestone {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private int number;

    @Column(nullable = false)
    private GitHubPropertyState state;

    @Column(nullable = false)
    private String title;

    @OneToMany(mappedBy = "milestone")
    private Set<Issue> issues;
}
