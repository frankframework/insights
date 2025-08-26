package org.frankframework.insights.common.shedlock;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Shedlock {
    @Id
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private OffsetDateTime lockUntil;

    @Column(nullable = false)
    private OffsetDateTime lockedAt;

    @Column(nullable = false)
    private String lockedBy;
}
