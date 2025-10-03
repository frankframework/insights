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
    private static final long MAX_FILE_SIZE = 1024 * 1024 * 100; // 100 MB
    private static final long MAX_ARCHIVE_SIZE = 1024 * 1024 * 1024; // 1 GB
    private static final int MAX_ENTRIES = 1024;

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
                boolean hasFiles = stream.findFirst().isPresent();
                if (hasFiles) {
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
        Path normalizedDestDir = destDir.toAbsolutePath().normalize();
        ZipArchiveState state = new ZipArchiveState(0, 0);

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                state = validateArchiveLimits(zipEntry, state);
                Path validatedPath = validateZipEntryPath(zipEntry, normalizedDestDir);
                extractZipEntry(zis, zipEntry, validatedPath);

                zipEntry = zis.getNextEntry();
            }
        }
    }

    private ZipArchiveState validateArchiveLimits(ZipEntry entry, ZipArchiveState currentState) throws IOException {
        int newCount = currentState.entriesCount() + 1;
        if (newCount > MAX_ENTRIES) {
            throw new IOException("Archive contains too many entries.");
        }

        long entrySize = entry.getSize();
        if (entrySize > MAX_FILE_SIZE) {
            throw new IOException("Archive contains a file that is too large: " + entry.getName());
        }

        long newTotalSize = currentState.unCompressedSize() + entrySize;
        if (newTotalSize > MAX_ARCHIVE_SIZE) {
            throw new IOException("Archive is too large when uncompressed.");
        }

        return new ZipArchiveState(newTotalSize, newCount);
    }

    private Path validateZipEntryPath(ZipEntry zipEntry, Path normalizedDestDir) throws IOException {
        Path resolvedPath = normalizedDestDir.resolve(zipEntry.getName()).normalize();

        if (!resolvedPath.startsWith(normalizedDestDir)) {
            throw new IOException("Bad zip entry: " + zipEntry.getName() + " (Path Traversal attempt)");
        }
        return resolvedPath;
    }

    /**
     * Extracts a single ZipEntry to the specified file path.
     * @param zis the ZipInputStream positioned at the entry to extract
     * @param zipEntry the ZipEntry to extract
     * @param filePath the destination file path
     * @throws IOException if an I/O error occurs
     */
    private void extractZipEntry(InputStream zis, ZipEntry zipEntry, Path filePath) throws IOException {
        if (zipEntry.isDirectory()) {
            Files.createDirectories(filePath);
        } else {
            Files.createDirectories(filePath.getParent());
            Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
