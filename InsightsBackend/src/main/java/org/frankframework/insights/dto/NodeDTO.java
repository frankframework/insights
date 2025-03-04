package org.frankframework.insights.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NodeDTO<T> {
	private List<T> nodes;
}
