package org.frankframework.insights.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
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

    @ManyToOne
    private PullRequest pullRequest;
}
