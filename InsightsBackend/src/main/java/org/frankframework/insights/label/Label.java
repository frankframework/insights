package org.frankframework.insights.label;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import lombok.Getter;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class Label {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(nullable = false)
    private String color;
}
