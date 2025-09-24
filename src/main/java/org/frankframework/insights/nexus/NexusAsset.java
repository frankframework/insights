package org.frankframework.insights.nexus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NexusAsset(String downloadUrl, String path, ZonedDateTime lastModified) {}
