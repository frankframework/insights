package org.frankframework.insights.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NodeDTO<T> {
    private List<T> nodes;
}
