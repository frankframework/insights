package org.frankframework.insights.nexus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NexusItem(String version, List<NexusAsset> assets) {}
