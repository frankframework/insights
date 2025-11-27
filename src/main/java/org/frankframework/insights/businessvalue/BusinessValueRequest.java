package org.frankframework.insights.businessvalue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BusinessValueRequest(
        @NotBlank @Size(min = 1, max = MAX_TITLE_LENGTH) String title,
        @NotBlank @Size(min = 1, max = MAX_DESCRIPTION_LENGTH) String description) {

    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;
}
