package org.frankframework.insights.businessvalue;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record ConnectIssuesRequest(@NotNull @NotEmpty Set<String> issueIds) {}
