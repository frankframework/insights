package org.frankframework.insights.release;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ReleaseArtifactService {

    private static final Path ARCHIVE_DIR = Paths.get("release-archive");
    private static final String GITHUB_ZIP_URL_FORMAT =
            "https://github.com/frankframework/frankframework/archive/refs/tags/%s.zip";

    /**
     * Prepares the source code for a release by downloading and unpacking it.
     * Skips the download if the artifact directory already exists.
     *
     * @param release The release to prepare artifacts for.
     * @return The path to the directory containing the unpacked source code.
     */
	@Transactional
    public Path prepareReleaseArtifacts(Release release) throws IOException {
        Path releaseDir = ARCHIVE_DIR.resolve(release.getName());

        if (releaseDirectoryExists(releaseDir, release)) {
            return releaseDir;
        }

        String zipUrl = buildZipUrl(release);
        Path zipFile = downloadReleaseZip(releaseDir, zipUrl, release);
        unpackAndCleanup(zipFile, releaseDir, release);

        return releaseDir;
    }

    private boolean releaseDirectoryExists(Path releaseDir, Release release) throws IOException {
        if (Files.isDirectory(releaseDir)) {
            try (Stream<Path> stream = Files.list(releaseDir)) {
                if (stream.findFirst().isPresent()) {
                    log.info("Source code for release {} already exists, skipping download.", release.getName());
                    return true;
                }
            }
        }
        return false;
    }

    private String buildZipUrl(Release release) throws IOException {
        String tagName = release.getTagName();
        if (tagName == null || tagName.isBlank()) {
            throw new IOException("Release " + release.getName() + " is missing a tagName.");
        }
        return String.format(GITHUB_ZIP_URL_FORMAT, tagName);
    }

    private Path downloadReleaseZip(Path releaseDir, String zipUrl, Release release) throws IOException {
        log.info("Downloading source for {} from {}", release.getName(), zipUrl);
        Files.createDirectories(releaseDir);
        Path zipFile = releaseDir.resolve(release.getName() + ".zip");

        try (InputStream in = URI.create(zipUrl).toURL().openStream()) {
            Files.copy(in, zipFile, StandardCopyOption.REPLACE_EXISTING);
        }
        return zipFile;
    }

    private void unpackAndCleanup(Path zipFile, Path releaseDir, Release release) throws IOException {
        log.debug("Unpacking archive for {}", release.getName());
        unzip(zipFile, releaseDir);
        Files.delete(zipFile);
        log.debug("Successfully downloaded and unpacked source for {}", release.getName());
    }

    private void unzip(Path zipFile, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                extractZipEntry(zis, zipEntry, destDir);
                zipEntry = zis.getNextEntry();
            }
        }
    }

    private void extractZipEntry(ZipInputStream zis, ZipEntry zipEntry, Path destDir) throws IOException {
        Path entryPath = Paths.get(zipEntry.getName());

        if (shouldSkipEntry(entryPath)) {
            return;
        }

        Path newPath = resolveEntryPath(entryPath, destDir);

        if (zipEntry.isDirectory()) {
            Files.createDirectories(newPath);
        } else {
            extractFile(zis, newPath);
        }
    }

    private boolean shouldSkipEntry(Path entryPath) {
        return entryPath.getNameCount() <= 1;
    }

    private Path resolveEntryPath(Path entryPath, Path destDir) {
        return destDir.resolve(entryPath.subpath(1, entryPath.getNameCount()).toString());
    }

    private void extractFile(ZipInputStream zis, Path newPath) throws IOException {
        if (newPath.getParent() != null) {
            Files.createDirectories(newPath.getParent());
        }
        Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
    }
}
