package org.frankframework.insights.businessvalue;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.frankframework.insights.issue.Issue;
import org.frankframework.insights.release.Release;

@Entity
@Getter
@Setter
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"title", "release_id"})})
public class BusinessValue {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "release_id", nullable = false)
    private Release release;

    @OneToMany(mappedBy = "businessValue")
    @JsonManagedReference("businessValue-issue")
    private Set<Issue> issues = new HashSet<>();
}
