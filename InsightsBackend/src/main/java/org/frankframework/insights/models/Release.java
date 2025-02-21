package org.frankframework.insights.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "release")
public class Release {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "tag_name", nullable = false, unique = true)
    private String tagName;

    @Column(name = "major_version", nullable = false)
    private String majorVersion;

    @Column(name = "published_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime publishedAt;

    @Column(name = "pull_requests")
    @OneToMany(mappedBy = "release")
    private Set<PullRequest> pullRequests;
}
