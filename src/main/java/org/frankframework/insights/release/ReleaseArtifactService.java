package org.frankframework.insights.release;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ReleaseArtifactService {
    private static final String GITHUB_ZIP_URL_FORMAT =
            "https://github.com/frankframework/frankframework/archive/refs/tags/%s.zip";
    private static final int MAX_ENTRIES = 50000;
    private static final long MAX_UNCOMPRESSED_SIZE = 1024L * 1024 * 1024 * 4;
    private static final double COMPRESSION_RATIO_LIMIT = 1000.0;
    private static final int BUFFER_SIZE = 4096;

    private final String releaseArchiveDirectory;
    private final ReleaseRepository releaseRepository;

    public ReleaseArtifactService(
            @Value("${release.archive.directory:/release-archive}") String releaseArchiveDirectory,
            ReleaseRepository releaseRepository) {
        this.releaseArchiveDirectory = releaseArchiveDirectory;
        this.releaseRepository = releaseRepository;
    }

    @Transactional
    public Path prepareReleaseArtifacts(Release release) throws IOException {
        Path releaseDir = Paths.get(releaseArchiveDirectory).resolve(release.getName());

        if (releaseDirectoryExists(releaseDir, release)) {
            return releaseDir;
        }

        String zipUrl = buildZipUrl(release);
        Path zipFile = downloadReleaseZip(releaseDir, zipUrl, release);
        unpackAndCleanup(zipFile, releaseDir, release);

        return releaseDir;
    }

    /**
     * Deletes release artifact directories that no longer correspond to any release in the database.
     * This cleanup ensures that the file system stays in sync with the database after releases are deleted.
     */
    @Transactional
    public void deleteObsoleteReleaseArtifacts() {
        Path archiveDir = Paths.get(releaseArchiveDirectory);

        if (!Files.exists(archiveDir)) {
            log.debug("Release archive directory {} does not exist, skipping cleanup.", releaseArchiveDirectory);
            return;
        }

        try {
            Set<String> validReleaseNames =
                    releaseRepository.findAll().stream().map(Release::getName).collect(Collectors.toSet());

            List<Path> obsoleteDirectories;
            try (Stream<Path> stream = Files.list(archiveDir)) {
                obsoleteDirectories = stream.filter(Files::isDirectory)
                        .filter(dir ->
                                !validReleaseNames.contains(dir.getFileName().toString()))
                        .toList();
            }

            for (Path obsoleteDir : obsoleteDirectories) {
                try {
                    deleteDirectoryRecursively(obsoleteDir);
                    log.info("Deleted obsolete release artifact directory: {}", obsoleteDir.getFileName());
                } catch (IOException e) {
                    log.error("Failed to delete obsolete release artifact directory: {}", obsoleteDir, e);
                }
            }

            if (!obsoleteDirectories.isEmpty()) {
                log.info("Cleaned up {} obsolete release artifact directories.", obsoleteDirectories.size());
            } else {
                log.debug("No obsolete release artifact directories found.");
            }
        } catch (IOException e) {
            log.error("Error while cleaning up obsolete release artifacts in {}", releaseArchiveDirectory, e);
        }
    }

    /**
     * Recursively deletes a directory and all its contents.
     */
    private void deleteDirectoryRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (Stream<Path> walk = Files.walk(directory)) {
            walk.sorted((a, b) -> -a.compareTo(b)).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    log.error("Failed to delete {}", path, e);
                }
            });
        }
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

    /**
     * Securely unzips a file using the java.nio.file.FileSystem API for robust and safe traversal.
     */
    private void unzip(Path zipFile, Path destDir) throws IOException {
        Path normalizedDestDir = destDir.toAbsolutePath().normalize();
        AtomicInteger entryCount = new AtomicInteger(0);
        AtomicLong totalUncompressedSize = new AtomicLong(0);

        try (FileSystem zipFs = FileSystems.newFileSystem(zipFile, (ClassLoader) null);
                ZipFile zf = new ZipFile(zipFile.toFile())) {

            Path root = zipFs.getPath("/");
            try (Stream<Path> stream = Files.walk(root)) {
                // Use a standard for-loop to handle exceptions cleanly from the stream
                for (Path path : (Iterable<Path>) stream::iterator) {
                    if (entryCount.incrementAndGet() > MAX_ENTRIES) {
                        throw new IOException("Archive contains too many entries.");
                    }
                    processPath(path, normalizedDestDir, totalUncompressedSize, zf);
                }
            }
        }
    }

    /**
     * Processes a single path from the zip file system, handling directories and files.
     */
    private void processPath(Path pathInZip, Path destDir, AtomicLong totalUncompressedSize, ZipFile zf)
            throws IOException {
        Path validatedPath = validateAndStripPath(pathInZip, destDir);
        if (validatedPath == null) {
            return; // Skip top-level directory
        }

        if (Files.isDirectory(pathInZip)) {
            Files.createDirectories(validatedPath);
        } else {
            extractAndValidateFile(pathInZip, validatedPath, totalUncompressedSize, zf);
        }
    }

    /**
     * Validates and prepares the destination path, preventing path traversal (Zip Slip).
     */
    private Path validateAndStripPath(Path pathInZip, Path destDir) throws IOException {
        if (pathInZip.getNameCount() <= 1) {
            return null; // Ignore the root and GitHub's top-level directory
        }
        Path strippedPath = pathInZip.subpath(1, pathInZip.getNameCount());
        Path resolvedPath = destDir.resolve(strippedPath.toString()).normalize();
        if (!resolvedPath.startsWith(destDir)) {
            throw new IOException("Bad zip entry: " + pathInZip + " (Path Traversal attempt)");
        }
        return resolvedPath;
    }

    /**
     * Extracts a file while validating its size and compression ratio.
     */
    private void extractAndValidateFile(Path pathInZip, Path destFile, AtomicLong totalUncompressedSize, ZipFile zf)
            throws IOException {
        Files.createDirectories(destFile.getParent());
        // ZipFile needs the entry name without the leading '/' from the zip file system path
        String entryName =
                pathInZip.toString().startsWith("/") ? pathInZip.toString().substring(1) : pathInZip.toString();
        ZipEntry entry = zf.getEntry(entryName);
        if (entry == null) {
            throw new IOException("Could not find ZipEntry for path: " + pathInZip);
        }

        long totalEntrySize = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream in = zf.getInputStream(entry);
                var out = Files.newOutputStream(destFile)) {
            int nBytes;
            while ((nBytes = in.read(buffer)) > 0) {
                out.write(buffer, 0, nBytes);
                totalEntrySize += nBytes;

                if (totalUncompressedSize.addAndGet(nBytes) > MAX_UNCOMPRESSED_SIZE) {
                    throw new IOException("Archive is too large when uncompressed.");
                }

                validateCompressionRatio(entry, totalEntrySize);
            }
        }
    }

    /**
     * Checks if the compression ratio of an entry exceeds the defined limit.
     */
    private void validateCompressionRatio(ZipEntry zipEntry, long totalEntrySize) throws IOException {
        long compressedSize = zipEntry.getCompressedSize();
        if (compressedSize > 0) {
            double compressionRatio = (double) totalEntrySize / compressedSize;
            if (compressionRatio > COMPRESSION_RATIO_LIMIT) {
                throw new IOException(
                        "Compression ratio for entry " + zipEntry.getName() + " is too high (Zip Bomb suspected).");
            }
        }
    }
}
