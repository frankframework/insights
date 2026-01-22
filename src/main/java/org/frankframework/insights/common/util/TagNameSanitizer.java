package org.frankframework.insights.common.util;

public final class TagNameSanitizer {

    private TagNameSanitizer() {}

    public static String sanitize(String tagName) {
        if (tagName == null) {
            return null;
        }
        return tagName.replace("/", "-");
    }

    public static String sanitizeWithSuffix(String tagName, String suffix) {
        if (tagName == null) {
            return null;
        }
        return sanitize(tagName) + suffix;
    }

    public static String sanitizeWithPrefix(String tagName, String prefix) {
        if (tagName == null) {
            return null;
        }
        return prefix + sanitize(tagName);
    }

    public static String sanitizeWithPrefixAndSuffix(String tagName, String prefix, String suffix) {
        if (tagName == null) {
            return null;
        }
        return prefix + sanitize(tagName) + suffix;
    }
}
