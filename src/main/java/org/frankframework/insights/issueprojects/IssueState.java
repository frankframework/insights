package org.frankframework.insights.issueprojects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.frankframework.insights.issue.Issue;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class IssueState {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String color;

    private String description;

    @OneToMany(mappedBy = "issueState")
    @JsonManagedReference("issueState-issue")
    private Set<Issue> issues;
}
