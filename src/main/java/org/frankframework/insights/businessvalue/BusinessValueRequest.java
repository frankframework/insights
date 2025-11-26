package org.frankframework.insights.businessvalue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BusinessValueRequest(
        @NotBlank @Size(min = 1, max = 255) String name,
        @NotBlank @Size(min = 1, max = 2000) String description) {}
