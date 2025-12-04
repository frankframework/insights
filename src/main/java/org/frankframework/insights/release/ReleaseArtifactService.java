package org.frankframework.insights.release;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ReleaseArtifactService {

    private static final String GITHUB_ZIP_URL_FORMAT =
            "https://github.com/frankframework/frankframework/archive/refs/tags/%s.zip";
    private static final String TRIVY_CACHE_DIR_NAME = ".trivy-cache";

    private static final int MAX_ENTRIES = 50_000;
    private static final long MAX_UNCOMPRESSED_SIZE = 4L * 1024 * 1024 * 1024; // 4GB
    private static final double COMPRESSION_RATIO_LIMIT = 1000.0;
    private static final int BUFFER_SIZE = 4096;

    private static final String SKIP_FILES_PATTERN = String.join(
            ",",
            "**/*.properties",
            "**/*.sh",
            "**/*.bat",
            "**/*.java",
            "**/*.ts",
            "**/*.js",
            "**/*.html",
            "**/*.css",
            "**/*.scss",
            "**/*.xslt",
            "**/*.rtf",
            "**/*.md",
            "src/test/**",
            ".mvn/**",
            "*.iml",
            ".idea/**",
            ".vscode/**",
            ".git/**",
            ".gitignore",
            "node_modules/**",
            "dist/**",
            "e2e/**",
            "angular.json",
            "browserslist",
            "karma.conf.js",
            "tsconfig.app.json",
            "tsconfig.json",
            "tsconfig.spec.json",
            "tslint.json",
            "**/*.txt",
            "**/*.log",
            "**/docs/**",
            "**/documentation/**",
            "**/*.gif",
            "**/*.png",
            "**/*.jpg",
            "**/*.jpeg",
            "**/*.svg",
            "**/*.tiff",
            "**/*.tif",
            "**/*.bmp",
            "**/*.ico",
            "**/*.woff",
            "**/*.woff2",
            "**/*.eot",
            "**/*.ttf",
            "**/*.pdf",
            "**/*.ppt",
            "**/*.doc",
            "**/*.docm",
            "**/*.xls",
            "**/*.xlsx",
            "**/*.eml",
            "**/*.msg",
            "**/*.zip",
            "**/*.map",
            "**/*.drawio",
            "**/*.xsd",
            "**/*.wsdl",
            "**/*.sql",
            "**/*.jks",
            "**/*.p12",
            "**/*.pfx",
            "**/*.cer",
            "**/*.key",
            "**/*.crt",
            "**/*.crl",
            "**/*.asc",
            "**/*.mjs");

    private final String releaseArchiveDirectory;
    private final ReleaseRepository releaseRepository;
    private List<PathMatcher> skipMatchers;

    public ReleaseArtifactService(
            @Value("${release.archive.directory:/release-archive}") String releaseArchiveDirectory,
            ReleaseRepository releaseRepository) {
        this.releaseArchiveDirectory = releaseArchiveDirectory;
        this.releaseRepository = releaseRepository;
    }

    @PostConstruct
    public void init() {
        this.skipMatchers = initializePathMatchers();
    }

    @Transactional
    public Path prepareReleaseArtifacts(Release release) throws IOException {
        Path releaseDir = Paths.get(releaseArchiveDirectory).resolve(release.getName());

        if (releaseDirectoryExists(releaseDir)) {
            log.info("Source code for release {} already exists.", release.getName());
            optimizeDirectoryStorage(releaseDir);
            return releaseDir;
        }

        Path zipFile = downloadReleaseZip(release);
        unpackAndCleanup(zipFile, releaseDir, release);
        optimizeDirectoryStorage(releaseDir);

        return releaseDir;
    }

    private Path downloadReleaseZip(Release release) throws IOException {
        String tagName = release.getTagName();
        if (tagName == null || tagName.isBlank()) {
            throw new IOException("Release " + release.getName() + " is missing a tagName.");
        }

        String zipUrl = String.format(GITHUB_ZIP_URL_FORMAT, tagName);
        log.info("Downloading source for {} from {}", release.getName(), zipUrl);

        Path releaseDir = Paths.get(releaseArchiveDirectory).resolve(release.getName());
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
        int entryCount = 0;
        long totalUncompressedSize = 0;
        int skippedCount = 0;

        try (FileSystem zipFs = FileSystems.newFileSystem(zipFile, (ClassLoader) null);
                ZipFile zf = new ZipFile(zipFile.toFile())) {

            try (Stream<Path> stream = Files.walk(zipFs.getPath("/"))) {
                for (Path pathInZip : (Iterable<Path>) stream::iterator) {

                    if (pathInZip.getNameCount() <= 1) continue;
                    Path relativePath = Paths.get(
                            pathInZip.subpath(1, pathInZip.getNameCount()).toString());

                    if (shouldSkipFile(relativePath)) {
                        skippedCount++;
                        continue;
                    }

                    if (++entryCount > MAX_ENTRIES) {
                        throw new IOException("Archive contains too many entries.");
                    }

                    totalUncompressedSize =
                            processZipEntry(zf, pathInZip, relativePath, normalizedDestDir, totalUncompressedSize);
                }
            }
        }
        log.info("Unzip complete. Skipped {} irrelevant files.", skippedCount);
    }

    private long processZipEntry(ZipFile zf, Path pathInZip, Path relativePath, Path destDir, long currentTotalSize)
            throws IOException {
        Path destPath = destDir.resolve(relativePath).normalize();
        if (!destPath.startsWith(destDir)) {
            throw new IOException("Bad zip entry (Zip Slip attempt): " + pathInZip);
        }

        if (Files.isDirectory(pathInZip)) {
            Files.createDirectories(destPath);
            return currentTotalSize;
        } else {
            return extractFile(zf, pathInZip.toString(), destPath, currentTotalSize);
        }
    }

    private long extractFile(ZipFile zf, String zipPathString, Path destFile, long currentTotalSize)
            throws IOException {
        String entryName = zipPathString.startsWith("/") ? zipPathString.substring(1) : zipPathString;
        ZipEntry entry = zf.getEntry(entryName);
        if (entry == null) throw new IOException("Could not find ZipEntry: " + entryName);

        Files.createDirectories(destFile.getParent());

        try (InputStream in = zf.getInputStream(entry);
                var out = Files.newOutputStream(destFile)) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int nBytes;
            long entrySize = 0;

            while ((nBytes = in.read(buffer)) > 0) {
                out.write(buffer, 0, nBytes);
                entrySize += nBytes;
                currentTotalSize += nBytes;

                if (currentTotalSize > MAX_UNCOMPRESSED_SIZE) {
                    throw new IOException("Archive is too large when uncompressed.");
                }

                long compressedSize = entry.getCompressedSize();
                if (compressedSize > 0 && (double) entrySize / compressedSize > COMPRESSION_RATIO_LIMIT) {
                    throw new IOException("Compression ratio too high.");
                }
            }
        }
        return currentTotalSize;
    }

    private void optimizeDirectoryStorage(Path directory) {
        if (!Files.exists(directory)) return;

        log.debug("Optimizing storage: Scanning {} for files to remove...", directory);
        AtomicInteger deletedCount = new AtomicInteger(0);
        AtomicLong freedSpace = new AtomicLong(0);

        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                @NonNull
                public FileVisitResult preVisitDirectory(Path dir, @NonNull BasicFileAttributes attrs)
                        throws IOException {
                    Path relativePath = directory.relativize(dir);
                    if (shouldSkipFile(relativePath)) {
                        deleteSubtree(dir, deletedCount, freedSpace);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                @NonNull
                public FileVisitResult visitFile(Path file, @NonNull BasicFileAttributes attrs) throws IOException {
                    Path relativePath = directory.relativize(file);
                    if (shouldSkipFile(relativePath)) {
                        freedSpace.addAndGet(attrs.size());
                        Files.delete(file);
                        deletedCount.incrementAndGet();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            if (deletedCount.get() > 0) {
                log.info(
                        "Cleanup finished for {}: Removed {} items, freed {} bytes.",
                        directory.getFileName(),
                        deletedCount.get(),
                        freedSpace.get());
            }
        } catch (IOException e) {
            log.error("Error during cleanup of ignored files in {}", directory, e);
        }
    }

    private void deleteSubtree(Path root, AtomicInteger deletedCount, AtomicLong freedSpace) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            @NonNull
            public FileVisitResult visitFile(Path file, @NonNull BasicFileAttributes attrs) throws IOException {
                if (freedSpace != null) freedSpace.addAndGet(attrs.size());
                Files.delete(file);
                if (deletedCount != null) deletedCount.incrementAndGet();
                return FileVisitResult.CONTINUE;
            }

            @Override
            @NonNull
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                if (deletedCount != null) deletedCount.incrementAndGet();
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Transactional
    public void deleteObsoleteReleaseArtifacts() {
        Path archiveDir = Paths.get(releaseArchiveDirectory);
        if (!Files.exists(archiveDir)) return;

        try {
            Set<String> validReleaseNames =
                    releaseRepository.findAll().stream().map(Release::getName).collect(Collectors.toSet());

            try (Stream<Path> stream = Files.list(archiveDir)) {
                List<Path> obsoleteDirectories = stream.filter(Files::isDirectory)
                        .filter(dir ->
                                !validReleaseNames.contains(dir.getFileName().toString()))
                        .filter(dir ->
                                !TRIVY_CACHE_DIR_NAME.equals(dir.getFileName().toString()))
                        .toList();

                for (Path obsoleteDir : obsoleteDirectories) {
                    deleteRecursively(obsoleteDir);
                    log.info("Deleted obsolete release artifact directory: {}", obsoleteDir.getFileName());
                }
            }
        } catch (IOException e) {
            log.error("Error while cleaning up obsolete release artifacts in {}", releaseArchiveDirectory, e);
        }
    }

    private void deleteRecursively(Path path) throws IOException {
        deleteSubtree(path, null, null);
    }

    private boolean releaseDirectoryExists(Path releaseDir) {
        return Files.isDirectory(releaseDir) && Files.exists(releaseDir);
    }

    private List<PathMatcher> initializePathMatchers() {
        if (SKIP_FILES_PATTERN.isBlank()) return Collections.emptyList();
        FileSystem fs = FileSystems.getDefault();
        return Arrays.stream(SKIP_FILES_PATTERN.split(","))
                .filter(s -> !s.isBlank())
                .map(String::trim)
                .map(p -> fs.getPathMatcher("glob:" + p))
                .collect(Collectors.toList());
    }

    private boolean shouldSkipFile(Path relativePath) {
        for (PathMatcher matcher : skipMatchers) {
            if (matcher.matches(relativePath)) return true;
        }
        return false;
    }
}
