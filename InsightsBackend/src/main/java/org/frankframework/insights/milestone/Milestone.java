package org.frankframework.insights.milestone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.frankframework.insights.github.GitHubPropertyState;
import org.frankframework.insights.issue.Issue;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Milestone {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private int number;

    @Column(nullable = false, unique = true)
    private String title;

    @Column(nullable = false)
    private GitHubPropertyState state;

    @OneToMany(mappedBy = "milestone")
    @JsonManagedReference("milestone-issue")
    private Set<Issue> issues;
}
