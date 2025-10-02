package org.frankframework.insights.release;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ReleaseDTO(String id, String tagName, String name, OffsetDateTime publishedAt) {

    private static final Pattern RC_PATTERN = Pattern.compile("-RC\\d+", Pattern.CASE_INSENSITIVE);
    private static final Pattern BETA_PATTERN = Pattern.compile("-B\\d+", Pattern.CASE_INSENSITIVE);
    private static final Pattern VERSION_PATTERN = Pattern.compile("v(\\d+)\\.(\\d+)");
    private static final int MAJOR = 1;
    private static final int MINOR = 2;

    /**
     * Checks if this release is valid (not a release candidate or beta release).
     * Filters out releases with patterns like -RCX or -BX.
     *
     * @return True if the release is valid, false otherwise.
     */
    public boolean isValid() {
        if (name == null) {
            return false;
        }
        return !RC_PATTERN.matcher(name).find() && !BETA_PATTERN.matcher(name).find();
    }

    /**
     * Extracts the major and minor version from the tag name.
     *
     * @return An Optional containing the major and minor version, or empty if not found.
     */
    public Optional<String> extractMajorMinor() {
        if (tagName == null) {
            return Optional.empty();
        }
        Matcher matcher = VERSION_PATTERN.matcher(tagName);
        return matcher.find() ? Optional.of(matcher.group(MAJOR) + "." + matcher.group(MINOR)) : Optional.empty();
    }
}
