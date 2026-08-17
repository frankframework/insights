package org.frankframework.insights.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TagNameSanitizerTest {

    public static Stream<Arguments> sanitizeTestCases() {
        return Stream.of(
                Arguments.of("feature/snapshot", "feature-snapshot"),
                Arguments.of("release/v1/beta", "release-v1-beta"),
                Arguments.of("v1.0.0", "v1.0.0"),
                Arguments.of("hotfix/bug/fix", "hotfix-bug-fix"),
                Arguments.of("/leading/slash", "-leading-slash"),
                Arguments.of("trailing/slash/", "trailing-slash-"),
                Arguments.of("no-slash-at-all", "no-slash-at-all"),
                Arguments.of("", ""));
    }

    public static Stream<Arguments> sanitizeWithSuffixTestCases() {
        return Stream.of(
                Arguments.of("feature/snapshot", ".zip", "feature-snapshot.zip"),
                Arguments.of("release/v1/beta", ".zip", "release-v1-beta.zip"),
                Arguments.of("v1.0.0", ".zip", "v1.0.0.zip"),
                Arguments.of("v8.0.0-SNAPSHOT", ".zip", "v8.0.0-SNAPSHOT.zip"));
    }

    public static Stream<Arguments> sanitizeWithPrefixTestCases() {
        return Stream.of(
                Arguments.of("feature/snapshot", "extracted-", "extracted-feature-snapshot"),
                Arguments.of("release/v1/beta", "extracted-", "extracted-release-v1-beta"),
                Arguments.of("v1.0.0", "extracted-", "extracted-v1.0.0"),
                Arguments.of("v8.0.0", "scan-", "scan-v8.0.0"));
    }

    public static Stream<Arguments> sanitizeWithPrefixAndSuffixTestCases() {
        return Stream.of(
                Arguments.of("feature/snapshot", "prefix-", "-suffix", "prefix-feature-snapshot-suffix"),
                Arguments.of("release/v1", "scan-", ".zip", "scan-release-v1.zip"),
                Arguments.of("v1.0.0", "", ".tar.gz", "v1.0.0.tar.gz"));
    }

    @ParameterizedTest
    @MethodSource("sanitizeTestCases")
    public void sanitize_shouldReplaceSlashesWithDashes(String input, String expected) {
        String result = TagNameSanitizer.sanitize(input);
        assertEquals(expected, result);
    }

    @ParameterizedTest
    @MethodSource("sanitizeWithSuffixTestCases")
    public void sanitizeWithSuffix_shouldReplaceSlashesAndAppendSuffix(String input, String suffix, String expected) {
        String result = TagNameSanitizer.sanitizeWithSuffix(input, suffix);
        assertEquals(expected, result);
    }

    @ParameterizedTest
    @MethodSource("sanitizeWithPrefixTestCases")
    public void sanitizeWithPrefix_shouldReplaceSlashesAndPrependPrefix(String input, String prefix, String expected) {
        String result = TagNameSanitizer.sanitizeWithPrefix(input, prefix);
        assertEquals(expected, result);
    }

    @ParameterizedTest
    @MethodSource("sanitizeWithPrefixAndSuffixTestCases")
    public void sanitizeWithPrefixAndSuffix_shouldReplaceSlashesAndAddPrefixAndSuffix(
            String input, String prefix, String suffix, String expected) {
        String result = TagNameSanitizer.sanitizeWithPrefixAndSuffix(input, prefix, suffix);
        assertEquals(expected, result);
    }
}
