package org.frankframework.insights.models;

import jakarta.persistence.*;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "label")
public class Label {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String name;

    @ManyToMany(mappedBy = "labels")
    private Set<Issue> issues;

    @ManyToMany(mappedBy = "labels")
    private Set<PullRequest> pullRequests;
}
