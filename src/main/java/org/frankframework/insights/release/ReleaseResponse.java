package org.frankframework.insights.release;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import org.frankframework.insights.branch.BranchResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReleaseResponse(
        String id,
        String tagName,
        String name,
        OffsetDateTime publishedAt,
        OffsetDateTime lastScanned,
        String commitSha,
        BranchResponse branch) {}
