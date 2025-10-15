package org.frankframework.insights.issueprojects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.frankframework.insights.issue.Issue;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class IssuePriority {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String color;

    private String description;

    @OneToMany(mappedBy = "issuePriority")
    @JsonManagedReference("issuePriority-issue")
    private Set<Issue> issues;
}
