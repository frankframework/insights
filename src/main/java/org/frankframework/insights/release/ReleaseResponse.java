package org.frankframework.insights.release;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.frankframework.insights.branch.BranchResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReleaseResponse(
        String id, String tagName, String name, OffsetDateTime publishedAt, String commitSha, BranchResponse branch) {}
