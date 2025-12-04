package org.frankframework.insights.release.releasecleanup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FileTreeDeleter {
    public void deleteTree(Path root, AtomicInteger deletedCount, AtomicLong freedSpace) throws IOException {
        Files.walkFileTree(root, new DeleteTreeVisitor(deletedCount, freedSpace));
    }

    public void deleteTreeRecursively(Path path) throws IOException {
        deleteTree(path, null, null);
    }
}
