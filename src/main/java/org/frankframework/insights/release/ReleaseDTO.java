package org.frankframework.insights.release;

import java.time.OffsetDateTime;
import java.util.regex.Pattern;

public record ReleaseDTO(String id, String tagName, String name, OffsetDateTime publishedAt) {

    private static final Pattern RC_PATTERN = Pattern.compile("-RC\\d+", Pattern.CASE_INSENSITIVE);
    private static final Pattern BETA_PATTERN = Pattern.compile("-B\\d+", Pattern.CASE_INSENSITIVE);

    /**
     * Checks if a release is valid (not a release candidate or beta release).
     * Filters out releases with patterns like -RCX or -BX in their name.
     *
     * @return True if the release is valid, false otherwise.
     */
    public boolean isValid() {
        if (name == null) {
            return false;
        }
        return !RC_PATTERN.matcher(name).find() && !BETA_PATTERN.matcher(name).find();
    }
}
