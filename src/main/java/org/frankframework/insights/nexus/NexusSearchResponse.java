package org.frankframework.insights.nexus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NexusSearchResponse(List<NexusItem> items) {}
