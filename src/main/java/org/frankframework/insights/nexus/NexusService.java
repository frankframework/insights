package org.frankframework.insights.nexus;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.release.Release;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class NexusService {
    private final RestTemplate restTemplate;

    public NexusService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Set<String> fetchAllReleaseVersions() {
        log.info("Fetching release versions from Nexus repository...");
        String apiUrl =
                "https://nexus.frankframework.org/service/rest/v1/search?repository=releases&group=org.frankframework&name=frankframework-parent";

        try {
            NexusSearchResponse response = restTemplate.getForObject(apiUrl, NexusSearchResponse.class);

            if (response != null && response.items() != null) {
                Set<String> versions = response.items().stream()
                        .map(item -> item.version().replace("-frankdoc", ""))
                        .collect(Collectors.toSet());
                log.info("Found {} release versions in Nexus", versions.size());
                return versions;
            }
        } catch (Exception e) {
            log.error("Failed to fetch release versions from Nexus: {}", e.getMessage(), e);
        }
        return Set.of();
    }

    public Optional<String> findBestPomUrlForRelease(Release release) {
        String baseVersion = release.getName().replaceAll("^v", "");
        log.debug("Looking for POM for release {} (base version: {})", release.getName(), baseVersion);

        return findPomUsingRegexMatching(baseVersion, release);
    }

    private Optional<String> findPomUsingRegexMatching(String baseVersion, Release release) {
        log.debug("Using regex-based version matching for release {}", release.getName());

        // Use the new correct API endpoint
        String apiUrl =
                "https://nexus.frankframework.org/service/rest/v1/search?repository=releases&group=org.frankframework&name=frankframework-parent";

        try {
            NexusSearchResponse response = restTemplate.getForObject(apiUrl, NexusSearchResponse.class);

            if (response == null || response.items() == null) {
                log.warn("No response or items from Nexus API for release {}", release.getName());
                return Optional.empty();
            }

            Pattern nightlyPattern = Pattern.compile("^(.+)-(\\d{8})\\.(\\d{6})$");
            Matcher nightlyMatcher = nightlyPattern.matcher(baseVersion);
            if (nightlyMatcher.matches()) {
                String versionBase = nightlyMatcher.group(1);
                String datePrefix = nightlyMatcher.group(2);
                String originalTime = nightlyMatcher.group(3);
                String regexPattern = versionBase + "-" + datePrefix + "\\.(\\d{6})";

                log.debug(
                        "Searching for closest nightly version on date {} with target time {} for release {}",
                        datePrefix,
                        originalTime,
                        release.getName());

                LocalTime targetTime = LocalTime.parse(originalTime, DateTimeFormatter.ofPattern("HHmmss"));

                Optional<NexusItem> closestItem = response.items().stream()
                        .filter(item -> {
                            Matcher itemMatcher = Pattern.compile(regexPattern).matcher(item.version());
                            return itemMatcher.matches();
                        })
                        .min(Comparator.comparing(item -> {
                            Matcher itemMatcher = Pattern.compile(regexPattern).matcher(item.version());
                            if (itemMatcher.matches()) {
                                String itemTime = itemMatcher.group(1);
                                LocalTime nexusTime = LocalTime.parse(itemTime, DateTimeFormatter.ofPattern("HHmmss"));
                                return Math.abs(
                                        Duration.between(targetTime, nexusTime).toMinutes());
                            }
                            return Long.MAX_VALUE;
                        }));

                Optional<String> closestMatch = closestItem.flatMap(item -> item.assets().stream()
                        .filter(asset -> asset.path().endsWith(".pom"))
                        .findFirst()
                        .map(NexusAsset::downloadUrl));

                if (closestMatch.isPresent()) {
                    log.info(
                            "Found closest nightly POM match for release {}: {}",
                            release.getName(),
                            closestMatch.get());
                    return closestMatch;
                }
            }

            // Try exact version match first
            Optional<String> exactMatch = response.items().stream()
                    .filter(item -> baseVersion.equals(item.version()))
                    .flatMap(item -> item.assets().stream())
                    .filter(asset -> asset.path().endsWith(".pom"))
                    .findFirst()
                    .map(NexusAsset::downloadUrl);

            if (exactMatch.isPresent()) {
                log.info("Found exact version match POM for release {}: {}", release.getName(), exactMatch.get());
                return exactMatch;
            }

            // Fallback: find closest match by timestamp if available
            if (release.getPublishedAt() != null) {
                return response.items().stream()
                        .flatMap(item -> item.assets().stream())
                        .filter(asset -> asset.path().endsWith(".pom"))
                        .min(Comparator.comparing(asset -> Duration.between(
                                        release.getPublishedAt().toInstant(),
                                        asset.lastModified().toInstant())
                                .abs()))
                        .map(NexusAsset::downloadUrl);
            }

            log.warn("No matching POM found for release {}", release.getName());
            return Optional.empty();

        } catch (Exception e) {
            log.error("Error during regex-based POM search for release {}: {}", release.getName(), e.getMessage());
            return Optional.empty();
        }
    }
}
