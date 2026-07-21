package org.frankframework.insights.release;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;
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
            if (isValidZip(zipPath)) {
                return zipPath;
            }
            log.warn(
                    "Cached ZIP for {} is corrupt (likely a truncated download), re-downloading: {}", tagName, zipPath);
            Files.deleteIfExists(zipPath);
        }

        log.info("ZIP not found op storage, downloading: {}", tagName);
        String url = String.format(GITHUB_ZIP_URL_FORMAT, tagName);
        Path tempZipPath = archiveDir.resolve(safeFileName + ".tmp");
        try (InputStream in = openDownloadStream(url)) {
            Files.copy(in, tempZipPath, StandardCopyOption.REPLACE_EXISTING);

            if (!isValidZip(tempZipPath)) {
                throw new IOException("Downloaded ZIP for " + tagName + " is not a valid archive");
            }

            moveIntoPlace(tempZipPath, zipPath);
            log.info("ZIP succesfully downloading to storage: {}", zipPath);
        } catch (IOException | URISyntaxException e) {
            try {
                Files.deleteIfExists(tempZipPath);
            } catch (IOException ignored) {
            }
            throw new IOException("Could not download release ZIP for " + tagName, e);
        }
        return zipPath;
    }

    private void moveIntoPlace(Path tempZipPath, Path zipPath) throws IOException {
        try {
            Files.move(tempZipPath, zipPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempZipPath, zipPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    protected InputStream openDownloadStream(String url) throws IOException, URISyntaxException {
        return new URI(url).toURL().openStream();
    }

    protected boolean isValidZip(Path path) {
        try (ZipFile ignored = new ZipFile(path.toFile())) {
            return true;
        } catch (IOException _) {
            return false;
        }
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
                boolean isOrphanedTempFile = fileName.endsWith(".tmp");
                boolean isObsoleteZip =
                        fileName.endsWith(".zip") && !activeReleaseTags.contains(fileName.replace(".zip", ""));

                if (isOrphanedTempFile || isObsoleteZip) {
                    try {
                        log.info("Deletion of obsolete release artifact: {}", fileName);
                        fileTreeDeleter.deleteTreeRecursively(path);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("Interrupted while deleting file: {}", fileName, e);
                    } catch (Exception e) {
                        log.error("Could not delete file: {}", fileName, e);
                    }
                }
            });
        } catch (IOException e) {
            log.error("Error while cleaning up release archive folder", e);
        }
    }
}
