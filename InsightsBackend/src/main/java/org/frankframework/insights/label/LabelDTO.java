package org.frankframework.insights.label;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LabelDTO {
    public String id;
    public String name;
    public String description;
    public String color;
}
