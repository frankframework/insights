package org.frankframework.insights.issueprojects;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.frankframework.insights.issue.Issue;

@Entity
@Getter
@Setter
public class IssuePriority extends IssueAttribute {
    @OneToMany(mappedBy = "issuePriority")
    @JsonManagedReference("issuePriority-issue")
    private Set<Issue> issues;
}
