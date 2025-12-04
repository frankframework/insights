package org.frankframework.insights.release;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.common.entityconnection.releasevulnerability.ReleaseVulnerability;

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

    private OffsetDateTime lastScanned;

    @ManyToOne
    private Branch branch;

    @OneToMany(mappedBy = "release", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReleaseVulnerability> releaseVulnerabilities = new ArrayList<>();
}
