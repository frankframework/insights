package org.frankframework.insights.businessvalue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.frankframework.insights.issue.Issue;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class BusinessValue {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "businessValue")
    @JsonManagedReference("businessValue-issue")
    private Set<Issue> issues = new HashSet<>();
}
