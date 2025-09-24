package org.frankframework.shared.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

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

    @ManyToOne
    private Branch branch;
}
