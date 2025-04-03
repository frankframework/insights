package org.frankframework.insights.release;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.common.entityconnection.ReleaseCommit;
import org.frankframework.insights.common.entityconnection.ReleasePullRequest;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Release {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String tagName;

    @Column(nullable = false, unique = true)
    private String name;

    private OffsetDateTime publishedAt;

    @Column(nullable = false, unique = true)
    private String commitSha;

    @ManyToOne
    private Branch branch;

    @OneToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    private Set<ReleaseCommit> releaseCommits;

	@OneToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
	private Set<ReleasePullRequest> releasePullRequests;
}
