package org.frankframework.webapp.common.entityconnection.releasepullrequest;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.frankframework.shared.entity.Release;
import org.frankframework.webapp.pullrequest.PullRequest;

@Entity
@Getter
@Setter
@NoArgsConstructor
@IdClass(ReleasePullRequestId.class)
public class ReleasePullRequest {
    @Id
    @ManyToOne
    @JoinColumn(nullable = false)
    private Release release;

    @Id
    @ManyToOne(cascade = {CascadeType.MERGE})
    @JoinColumn(nullable = false)
    private PullRequest pullRequest;

    public ReleasePullRequest(Release release, PullRequest pullRequest) {
        this.release = release;
        this.pullRequest = pullRequest;
    }
}
