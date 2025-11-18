package org.frankframework.insights.businessvalue;

import jakarta.validation.constraints.NotBlank;

public record BusinessValueRequest(@NotBlank String name, @NotBlank String description) {}
