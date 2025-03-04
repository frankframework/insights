package org.frankframework.insights.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.Set;
import lombok.Getter;

@Entity
@Table(name = "label")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class Label {
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String color;

    @ManyToMany(mappedBy = "labels")
    private Set<Issue> issues;

    @ManyToMany(mappedBy = "labels")
    private Set<PullRequest> pullRequests;
}
