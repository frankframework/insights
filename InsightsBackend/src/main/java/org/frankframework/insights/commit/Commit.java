package org.frankframework.insights.commit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.frankframework.insights.common.entityconnection.ReleaseCommit;
import org.frankframework.insights.common.entityconnection.branchcommit.BranchCommit;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Commit {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String sha;

    @Lob
    @Column(nullable = false)
    private String message;

    private OffsetDateTime committedDate;

    @OneToMany
    private Set<BranchCommit> branchCommits;

    @OneToMany
    private Set<ReleaseCommit> releaseCommits;
}
