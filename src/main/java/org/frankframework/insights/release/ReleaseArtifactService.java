package org.frankframework.insights.release;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    public Path prepareReleaseArtifacts(Release release) throws IOException {
        Path releaseDir = ARCHIVE_DIR.resolve(release.getName());

        if (Files.isDirectory(releaseDir) && Files.list(releaseDir).findFirst().isPresent()) {
            log.info("Source code for release {} already exists, skipping download.", release.getName());
            return releaseDir;
        }

        String tagName = release.getTagName();
        if (tagName == null || tagName.isBlank()) {
            throw new IOException("Release " + release.getName() + " is missing a tagName.");
        }
        String zipUrl = String.format(GITHUB_ZIP_URL_FORMAT, tagName);

        log.info("Downloading source for {} from {}", release.getName(), zipUrl);
        Files.createDirectories(releaseDir);
        Path zipFile = releaseDir.resolve(release.getName() + ".zip");

        try (InputStream in = URI.create(zipUrl).toURL().openStream()) {
            Files.copy(in, zipFile, StandardCopyOption.REPLACE_EXISTING);
        }

        log.debug("Unpacking archive for {}", release.getName());
        unzip(zipFile, releaseDir);

        Files.delete(zipFile);
        log.debug("Successfully downloaded and unpacked source for {}", release.getName());
        return releaseDir;
    }

    /**
     * Helper method to unzip a file. It handles archives with a single top-level directory.
     */
    private void unzip(Path zipFile, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                Path entryPath = Paths.get(zipEntry.getName());

                // Skip if path doesn't have enough components to strip the top-level directory
                if (entryPath.getNameCount() <= 1) {
                    zipEntry = zis.getNextEntry();
                    continue;
                }

                Path newPath = destDir.resolve(
                        entryPath.subpath(1, entryPath.getNameCount()).toString());

                if (zipEntry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    if (newPath.getParent() != null) {
                        Files.createDirectories(newPath.getParent());
                    }
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zipEntry = zis.getNextEntry();
            }
        }
    }
}
