package org.frankframework.webapp.issuetype;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.frankframework.webapp.issue.Issue;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class IssueType {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(nullable = false)
    private String color;

    @OneToMany(mappedBy = "issueType")
    @JsonManagedReference("issueType-issue")
    private Set<Issue> issues;
}
