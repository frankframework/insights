package org.frankframework.insights.businessvalue;

import java.util.Set;
import java.util.UUID;
import org.frankframework.insights.issue.IssueResponse;

public record BusinessValueResponse(UUID id, String title, String description, Set<IssueResponse> issues) {}
