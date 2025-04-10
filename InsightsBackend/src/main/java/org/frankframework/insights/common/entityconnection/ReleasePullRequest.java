package org.frankframework.insights.common.entityconnection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.frankframework.insights.pullrequest.PullRequest;
import org.frankframework.insights.release.Release;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
public class ReleasePullRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(nullable = false)
    @JsonIgnore
    private Release release;

    @ManyToOne(cascade = {CascadeType.MERGE})
    @JoinColumn(nullable = false)
    private PullRequest pullRequest;

    public ReleasePullRequest(Release release, PullRequest pullRequest) {
        this.release = release;
        this.pullRequest = pullRequest;
    }
}
