package org.frankframework.insights.businessvalue;

import jakarta.validation.constraints.NotBlank;

public record DuplicateBusinessValuesRequest(@NotBlank String sourceReleaseId) {}
