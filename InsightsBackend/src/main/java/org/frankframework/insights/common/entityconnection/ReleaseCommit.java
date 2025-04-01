package org.frankframework.insights.common.entityconnection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.frankframework.insights.commit.Commit;
import org.frankframework.insights.release.Release;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
public class ReleaseCommit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(nullable = false)
    @JsonIgnore
    private Release release;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Commit commit;

    public ReleaseCommit(Release release, Commit commit) {
        this.release = release;
        this.commit = commit;
    }
}
