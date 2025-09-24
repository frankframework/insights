package org.frankframework.insights.dependency;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.frankframework.insights.common.entityconnection.releasedependency.ReleaseDependencyRepository;
import org.frankframework.insights.nexus.NexusService;
import org.frankframework.insights.release.Release;
import org.frankframework.insights.release.ReleaseRepository;
import org.frankframework.insights.vulnerability.VulnerabilityService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DependencyService {

    private final NexusService nexusService;
    private final DependencyRepository dependencyRepository;
    private final ReleaseDependencyRepository releaseDependencyRepository;
    private final ReleaseRepository releaseRepository;
    private final VulnerabilityService vulnerabilityService;

    public DependencyService(
            NexusService nexusService,
            DependencyRepository dependencyRepository,
            ReleaseDependencyRepository releaseDependencyRepository,
            ReleaseRepository releaseRepository,
            VulnerabilityService vulnerabilityService) {
        this.nexusService = nexusService;
        this.dependencyRepository = dependencyRepository;
        this.releaseDependencyRepository = releaseDependencyRepository;
        this.releaseRepository = releaseRepository;
        this.vulnerabilityService = vulnerabilityService;
    }

    public void collectAndSaveDependencies(Release release) throws Exception {
        log.info("Starting dependency collection process for release: {}", release.getName());
        String pomUrl = nexusService
                .findBestPomUrlForRelease(release)
                .orElseThrow(() -> new RuntimeException("No POM file found in Nexus for release " + release.getName()));

        log.info("Found POM URL for release {}: {}", release.getName(), pomUrl);
        Path tempPomFile = downloadPomFromNexus(pomUrl);
        String cleanVersion = release.getName().replaceAll("^v", "");
        Path outputDir = Paths.get("dependency-archive/" + cleanVersion);
        Files.createDirectories(outputDir);

        log.info("Starting Maven dependency collection for release: {}", release.getId());
        ProcessBuilder pb = new ProcessBuilder(
                "mvn",
                "dependency:copy-dependencies",
                "-f",
                tempPomFile.toString(),
                "-DoutputDirectory=" + outputDir.toAbsolutePath());
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            log.error(
                    "Maven dependency collection failed for release {} with exit code: {}",
                    release.getName(),
                    exitCode);
            throw new RuntimeException("Maven dependency collection failed with exit code: " + exitCode);
        }
        log.info("Dependencies successfully collected for release {} in directory: {}", release.getName(), outputDir);

        analyzeAndSaveDependencies(release, outputDir);
        Files.delete(tempPomFile);
    }

    private void analyzeAndSaveDependencies(Release release, Path jarDirectory) throws IOException {
        log.info("Analyzing JAR dependencies for release {} from directory: {}", release.getName(), jarDirectory);
        try (Stream<Path> stream = Files.walk(jarDirectory)) {
            long totalJars = 0;
            long processedJars = 0;

            for (Path jarPath :
                    stream.filter(file -> file.toString().endsWith(".jar")).toList()) {
                totalJars++;
                Dependency dependency = parseGAV(jarPath);
                if (dependency.getGroupId() == null
                        || dependency.getArtifactId() == null
                        || dependency.getVersion() == null) {
                    log.debug("Skipping JAR {} - unable to extract GAV information", jarPath.getFileName());
                    continue;
                }

                Dependency dbDependency = dependencyRepository
                        .findByGroupIdAndArtifactIdAndVersion(
                                dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion())
                        .orElseGet(() -> {
                            log.debug(
                                    "Saving new dependency: {}:{}:{}",
                                    dependency.getGroupId(),
                                    dependency.getArtifactId(),
                                    dependency.getVersion());
                            return dependencyRepository.save(dependency);
                        });

                org.frankframework.insights.common.entityconnection.releasedependency.ReleaseDependency link =
                        new org.frankframework.insights.common.entityconnection.releasedependency.ReleaseDependency(
                                release, dbDependency);
                releaseDependencyRepository.save(link);
                processedJars++;
            }

            log.info(
                    "Dependency analysis completed for release {}: processed {} out of {} JAR files",
                    release.getName(),
                    processedJars,
                    totalJars);
        }
    }

    private Path downloadPomFromNexus(String pomUrl) throws IOException {
        log.debug("Downloading POM file from Nexus: {}", pomUrl);
        URL url = new URL(pomUrl);
        Path tempFile = Files.createTempFile("nexus-pom-", ".xml");

        try (InputStream in = url.openStream()) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Successfully downloaded POM file to temporary location: {}", tempFile);
        } catch (IOException e) {
            log.error("Failed to download POM file from Nexus URL {}: {}", pomUrl, e.getMessage());
            throw new IOException("Failed to download POM from Nexus: " + pomUrl, e);
        }
        return tempFile;
    }

    private Dependency parseGAV(Path jarPath) {
        Dependency dependency = new Dependency();
        dependency.setFileName(jarPath.getFileName().toString());
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            jarFile.stream()
                    .filter(entry -> entry.getName().matches("META-INF/maven/.*/pom.xml"))
                    .findFirst()
                    .ifPresent(pomEntry -> {
                        try (InputStream pomStream = jarFile.getInputStream(pomEntry)) {
                            Model model = new MavenXpp3Reader().read(pomStream);
                            dependency.setGroupId(
                                    model.getGroupId() != null
                                            ? model.getGroupId()
                                            : model.getParent().getGroupId());
                            dependency.setArtifactId(model.getArtifactId());
                            dependency.setVersion(
                                    model.getVersion() != null
                                            ? model.getVersion()
                                            : model.getParent().getVersion());
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {
        }
        return dependency;
    }

    /**
     * Executes dependency and CVE scanning for all releases.
     * Finds new releases from Nexus, onboards them, and scans all releases for vulnerabilities.
     */
    public void executeDependencyAndCveScan() {
        Set<String> newReleaseTags = findNewReleases();

        if (!newReleaseTags.isEmpty()) {
            log.info("Found new releases: {}", newReleaseTags);
            onboardNewReleases(newReleaseTags);
        } else {
            log.info("No new releases found");
        }

        scanAllReleasesForVulnerabilities();
    }

    private Set<String> findNewReleases() {
        log.info("Comparing Nexus releases with existing database releases to find new ones...");
        Set<String> nexusReleaseNames = nexusService.fetchAllReleaseVersions();
        Set<String> databaseReleaseNames = releaseRepository.findAllNames();
        log.info(
                "Found {} releases in Nexus, {} releases in database",
                nexusReleaseNames.size(),
                databaseReleaseNames.size());
        nexusReleaseNames.removeAll(databaseReleaseNames);
        return nexusReleaseNames;
    }

    private void onboardNewReleases(Set<String> newReleaseNames) {
        for (String name : newReleaseNames) {
            log.info("Onboarding new release from Nexus: {}", name);
            Release newRelease = new Release();
            newRelease.setId(name);
            newRelease.setTagName(name);
            newRelease.setName(name);
            newRelease.setPublishedAt(OffsetDateTime.now());
            releaseRepository.save(newRelease);
            log.debug("Saved new release {} to database", name);

            try {
                log.info("Starting dependency collection for Nexus release: {}", name);
                collectAndSaveDependencies(newRelease);
                log.info("Successfully completed dependency collection for Nexus release: {}", name);
            } catch (Exception e) {
                log.error("Failed to collect dependencies for Nexus release {}: {}", name, e.getMessage(), e);
            }
        }
    }

    private void scanAllReleasesForVulnerabilities() {
        log.info("Starting CVE scans for all releases...");
        List<Release> allReleases = releaseRepository.findAll();
        for (Release release : allReleases) {
            try {
                vulnerabilityService.scanRelease(release);
            } catch (Exception e) {
                log.error("Error during scanning of release {}: {}", release.getTagName(), e.getMessage());
            }
        }
    }
}
