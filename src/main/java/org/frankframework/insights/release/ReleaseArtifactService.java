package org.frankframework.insights.release;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.util.TagNameSanitizer;
import org.frankframework.insights.release.releasecleanup.FileTreeDeleter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReleaseArtifactService {
    private static final String GITHUB_ZIP_URL_FORMAT =
            "https://github.com/frankframework/frankframework/archive/refs/tags/%s.zip";

    private final String releaseArchiveDirectory;
    private final FileTreeDeleter fileTreeDeleter;
    private final ReleaseRepository releaseRepository;

    public ReleaseArtifactService(
            @Value("${release.archive.directory:/release-archive}") String releaseArchiveDirectory,
            ReleaseRepository releaseRepository) {
        this.releaseArchiveDirectory = releaseArchiveDirectory;
        this.fileTreeDeleter = new FileTreeDeleter();
        this.releaseRepository = releaseRepository;
    }

    public Path downloadReleaseZipToPvc(String tagName) throws IOException {
        Path archiveDir = Paths.get(releaseArchiveDirectory);
        if (!Files.exists(archiveDir)) Files.createDirectories(archiveDir);

        String safeFileName = TagNameSanitizer.sanitizeWithSuffix(tagName, ".zip");
        Path zipPath = archiveDir.resolve(safeFileName);

        if (Files.exists(zipPath)) {
            return zipPath;
        }

        log.info("ZIP not found op storage, downloading: {}", tagName);
        String url = String.format(GITHUB_ZIP_URL_FORMAT, tagName);
        try (InputStream in = new URI(url).toURL().openStream()) {
            Files.copy(in, zipPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("ZIP succesfully downloading to storage: {}", zipPath);
        } catch (Exception e) {
            try {
                Files.deleteIfExists(zipPath);
            } catch (IOException ignored) {
            }
            throw new IOException("Could not download release ZIP for " + tagName, e);
        }
        return zipPath;
    }

    public void deleteObsoleteReleaseArtifacts() {
        List<Release> allReleases = releaseRepository.findAll();
        Set<String> activeReleaseTags =
                allReleases.stream().map(Release::getTagName).collect(Collectors.toSet());

        Path dir = Paths.get(releaseArchiveDirectory);
        if (!Files.exists(dir)) return;

        try (Stream<Path> files = Files.list(dir)) {
            files.forEach(path -> {
                String fileName = path.getFileName().toString();
                if (fileName.endsWith(".zip")) {
                    String tagName = fileName.replace(".zip", "");
                    if (!activeReleaseTags.contains(tagName)) {
                        try {
                            log.info("Deletion of obsolete release artifact: {}", fileName);
                            fileTreeDeleter.deleteTreeRecursively(path);
                        } catch (Exception e) {
                            log.error("Could not delete file: {}", fileName, e);
                        }
                    }
                }
            });
        } catch (IOException e) {
            log.error("Error while cleaning up release archive folder", e);
        }
    }
}
