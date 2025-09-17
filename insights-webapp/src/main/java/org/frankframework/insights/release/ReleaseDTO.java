package org.frankframework.insights.release;

import java.time.OffsetDateTime;

public record ReleaseDTO(String id, String tagName, String name, OffsetDateTime publishedAt) {}
