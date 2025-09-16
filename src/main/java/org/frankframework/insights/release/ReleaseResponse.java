package org.frankframework.insights.release;

import java.time.OffsetDateTime;
import org.frankframework.insights.branch.BranchResponse;

public record ReleaseResponse(
        String id, String tagName, String name, OffsetDateTime publishedAt, String commitSha, BranchResponse branch) {}
