package org.frankframework.insights.common.entityconnection.releasedependency;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.frankframework.insights.dependency.Dependency;
import org.frankframework.insights.release.Release;

@Entity
@Getter
@Setter
@NoArgsConstructor
@IdClass(ReleaseDependencyId.class)
public class ReleaseDependency {
    @Id
    @ManyToOne
    private Release release;

    @Id
    @ManyToOne
    private Dependency dependency;

    public ReleaseDependency(Release release, Dependency dependency) {
        this.release = release;
        this.dependency = dependency;
    }
}
