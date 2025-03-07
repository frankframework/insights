package org.frankframework.insights.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "branch")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class Branch {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Setter
    @ManyToMany
    @JoinTable(
            name = "branch_commit",
            joinColumns = @JoinColumn(name = "branch_id"),
            inverseJoinColumns = @JoinColumn(name = "commit_id"))
    private Set<Commit> commits;
}
