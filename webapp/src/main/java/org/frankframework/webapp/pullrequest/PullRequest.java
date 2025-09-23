package org.frankframework.webapp.pullrequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.frankframework.webapp.milestone.Milestone;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class PullRequest {
    @Id
    private String id;

    @Column(nullable = false)
    private int number;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String url;

    private OffsetDateTime mergedAt;

    @ManyToOne
    private Milestone milestone;
}
