package org.frankframework.insights.release.releasecleanup;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import lombok.NonNull;

public class CleanupFileVisitor extends SimpleFileVisitor<Path> {
    private final Path baseDirectory;
    private final AtomicInteger deletedCount;
    private final AtomicLong freedSpace;
    private final Predicate<Path> shouldSkipFile;
    private final FileTreeDeleter fileTreeDeleter;

    public CleanupFileVisitor(
            Path baseDirectory,
            AtomicInteger deletedCount,
            AtomicLong freedSpace,
            Predicate<Path> shouldSkipFile,
            FileTreeDeleter fileTreeDeleter) {
        this.baseDirectory = baseDirectory;
        this.deletedCount = deletedCount;
        this.freedSpace = freedSpace;
        this.shouldSkipFile = shouldSkipFile;
        this.fileTreeDeleter = fileTreeDeleter;
    }

    @Override
    @NonNull
    public FileVisitResult preVisitDirectory(Path dir, @NonNull BasicFileAttributes attrs) throws IOException {
        Path relativePath = baseDirectory.relativize(dir);
        if (shouldSkipFile.test(relativePath)) {
            fileTreeDeleter.deleteTree(dir, deletedCount, freedSpace);
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    @NonNull
    public FileVisitResult visitFile(Path file, @NonNull BasicFileAttributes attrs) throws IOException {
        Path relativePath = baseDirectory.relativize(file);
        if (shouldSkipFile.test(relativePath)) {
            freedSpace.addAndGet(attrs.size());
            Files.delete(file);
            deletedCount.incrementAndGet();
        }
        return FileVisitResult.CONTINUE;
    }
}
