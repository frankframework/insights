package org.frankframework.insights.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "release")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class Release {
    @Id
    private String id;

    @Column(name = "tag_name", nullable = false, unique = true)
    private String tagName;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "published_at", columnDefinition = "TIMESTAMP")
    private OffsetDateTime publishedAt;

    @Setter
    @ManyToOne
    private Branch branch;

    @Setter
    @ManyToMany
    @JoinTable(
            name = "release_commit",
            joinColumns = @JoinColumn(name = "release_id"),
            inverseJoinColumns = @JoinColumn(name = "commit_id"))
    private Set<Commit> commits;
}
