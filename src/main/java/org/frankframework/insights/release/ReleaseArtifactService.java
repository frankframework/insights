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

    private static final int BUFFER_SIZE = 4096;
    private static final long MAX_ARCHIVE_SIZE = 1024L * 1024 * 1024;
    private static final int MAX_ENTRIES = 1024;
    private static final double COMPRESSION_RATIO_LIMIT = 10.0;

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
        unpackAndCleanup(zipFile, releaseDir);

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

    private void unpackAndCleanup(Path zipFile, Path releaseDir) throws IOException {
        log.debug("Unpacking archive for {}", zipFile.getFileName());
        unzip(zipFile, releaseDir);
        Files.delete(zipFile);
        log.debug("Successfully downloaded and unpacked source for {}", zipFile.getFileName());
    }

    /**
     * Securely unzips a file, managing security state as local variables to satisfy static analysis tools.
     */
    private void unzip(Path zipFile, Path destDir) throws IOException {
        Path normalizedDestDir = destDir.toAbsolutePath().normalize();
        long totalUncompressedSize = 0;
        int entryCount = 0;

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                if (++entryCount > MAX_ENTRIES) {
                    throw new IOException("Archive contains too many entries.");
                }
                totalUncompressedSize = processEntry(zis, zipEntry, normalizedDestDir, totalUncompressedSize);
            }
        }
    }

    /**
     * Processes a single zip entry, handling path validation and dispatching to directory or file extraction.
     * @return The new total uncompressed size of the archive.
     */
    private long processEntry(ZipInputStream zis, ZipEntry zipEntry, Path normalizedDestDir, long currentTotalSize)
            throws IOException {
        Path validatedPath = validateZipEntryPath(zipEntry, normalizedDestDir);
        if (validatedPath == null) {
            return currentTotalSize; // Skip and return size unchanged
        }

        if (zipEntry.isDirectory()) {
            Files.createDirectories(validatedPath);
            return currentTotalSize; // Directory created, return size unchanged
        }
        return extractFile(zis, zipEntry, validatedPath, currentTotalSize);
    }

    /**
     * Extracts a single file, performing security checks for size and compression ratio.
     * @return The new total uncompressed size of the archive after extraction.
     */
    private long extractFile(ZipInputStream zis, ZipEntry zipEntry, Path filePath, long currentTotalSize)
            throws IOException {
        Files.createDirectories(filePath.getParent());
        byte[] buffer = new byte[BUFFER_SIZE];
        long totalEntrySize = 0;
        long newTotalArchiveSize = currentTotalSize;

        try (var out = Files.newOutputStream(filePath)) {
            int nBytes;
            while ((nBytes = zis.read(buffer)) > 0) {
                out.write(buffer, 0, nBytes);
                totalEntrySize += nBytes;
                newTotalArchiveSize += nBytes;

                if (newTotalArchiveSize > MAX_ARCHIVE_SIZE) {
                    throw new IOException("Archive is too large when uncompressed.");
                }
                validateCompressionRatio(zipEntry, totalEntrySize);
            }
        }
        return newTotalArchiveSize;
    }

    /**
     * Validates a zip entry's path to prevent the "Zip Slip" vulnerability.
     */
    private Path validateZipEntryPath(ZipEntry zipEntry, Path normalizedDestDir) throws IOException {
        Path entryPath = Paths.get(zipEntry.getName());
        if (entryPath.getNameCount() <= 1) {
            return null; // Ignore GitHub's top-level directory
        }

        Path strippedPath = entryPath.subpath(1, entryPath.getNameCount());
        Path resolvedPath = normalizedDestDir.resolve(strippedPath.toString()).normalize();

        if (!resolvedPath.startsWith(normalizedDestDir)) {
            throw new IOException("Bad zip entry: " + zipEntry.getName() + " (Path Traversal attempt)");
        }
        return resolvedPath;
    }

    /**
     * Checks if the compression ratio of an entry exceeds the defined limit.
     */
    private void validateCompressionRatio(ZipEntry zipEntry, long totalEntrySize) throws IOException {
        if (zipEntry.getCompressedSize() > 0) {
            double compressionRatio = (double) totalEntrySize / zipEntry.getCompressedSize();
            if (compressionRatio > COMPRESSION_RATIO_LIMIT) {
                throw new IOException(
                        "Compression ratio for entry " + zipEntry.getName() + " is too high (Zip Bomb suspected).");
            }
        }
    }
}
