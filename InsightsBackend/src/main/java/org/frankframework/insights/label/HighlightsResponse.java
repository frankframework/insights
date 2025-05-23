package org.frankframework.insights.label;

import java.util.Map;
import java.util.Set;

public record HighlightsResponse(Map<LabelResponse, Long> AllHighlights, Set<LabelResponse> filteredHighlights) {}
