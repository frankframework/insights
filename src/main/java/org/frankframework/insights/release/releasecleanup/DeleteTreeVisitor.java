package org.frankframework.insights.release.releasecleanup;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.NonNull;

public class DeleteTreeVisitor extends SimpleFileVisitor<Path> {
    private final AtomicInteger deletedCount;
    private final AtomicLong freedSpace;

    public DeleteTreeVisitor(AtomicInteger deletedCount, AtomicLong freedSpace) {
        this.deletedCount = deletedCount;
        this.freedSpace = freedSpace;
    }

    @Override
    @NonNull
    public FileVisitResult visitFile(Path file, @NonNull BasicFileAttributes attrs) throws IOException {
        if (freedSpace != null) {
            freedSpace.addAndGet(attrs.size());
        }
        Files.delete(file);
        if (deletedCount != null) {
            deletedCount.incrementAndGet();
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    @NonNull
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (exc != null) {
            throw exc;
        }
        Files.delete(dir);
        if (deletedCount != null) {
            deletedCount.incrementAndGet();
        }
        return FileVisitResult.CONTINUE;
    }
}
