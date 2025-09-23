package org.frankframework.webapp.release;

import java.time.OffsetDateTime;
import org.frankframework.webapp.branch.BranchResponse;

public record ReleaseResponse(
        String id, String tagName, String name, OffsetDateTime publishedAt, String commitSha, BranchResponse branch) {}
