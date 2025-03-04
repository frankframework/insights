package org.frankframework.insights.dto;

import lombok.Getter;

public class InfoPageDTO {
	private final boolean hasNextPage;
	private final String endCursor;

	public InfoPageDTO(boolean hasNextPage, String endCursor) {
		this.hasNextPage = hasNextPage;
		this.endCursor = endCursor;
	}
}
