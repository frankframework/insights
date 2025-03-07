package org.frankframework.insights.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Set;
import lombok.Getter;

@Entity
@Table(name = "commit")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class Commit {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String sha;

    @Column(nullable = false)
    private String message;

    @Column(name = "timestamp", columnDefinition = "TIMESTAMP")
    private OffsetDateTime timestamp;

    @ManyToMany(mappedBy = "commits")
    private Set<Branch> branches;

    @ManyToOne
    private PullRequest pullRequest;
}
