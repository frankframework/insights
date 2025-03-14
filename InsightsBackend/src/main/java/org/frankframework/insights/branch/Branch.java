package org.frankframework.insights.branch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

import org.frankframework.insights.commit.Commit;

@Entity
@Table(name = "branch")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Branch {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToMany(
            fetch = FetchType.EAGER,
            cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinTable(
            name = "branch_commit",
            joinColumns = @JoinColumn(name = "branch_id"),
            inverseJoinColumns = @JoinColumn(name = "commit_id"))
    private Set<Commit> commits = new HashSet<>();
}
