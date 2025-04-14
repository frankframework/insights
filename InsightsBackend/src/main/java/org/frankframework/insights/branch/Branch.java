package org.frankframework.insights.branch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Branch {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String name;
}
