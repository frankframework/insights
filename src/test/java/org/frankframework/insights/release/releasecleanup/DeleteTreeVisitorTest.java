package org.frankframework.insights.release.releasecleanup;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DeleteTreeVisitorTest {

    @Test
    public void visitFile_shouldDeleteFileAndIncrementCount(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "test content");

        AtomicInteger deletedCount = new AtomicInteger(0);
        AtomicLong freedSpace = new AtomicLong(0);

        DeleteTreeVisitor visitor = new DeleteTreeVisitor(deletedCount, freedSpace);

        assertTrue(Files.exists(file));

        Files.walkFileTree(tempDir, visitor);

        assertFalse(Files.exists(file));
        assertFalse(Files.exists(tempDir));
        assertEquals(2, deletedCount.get());
        assertTrue(freedSpace.get() > 0);
    }

    @Test
    public void visitFile_withNullCounters_shouldStillDelete(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "test content");

        DeleteTreeVisitor visitor = new DeleteTreeVisitor(null, null);

        assertTrue(Files.exists(file));

        Files.walkFileTree(tempDir, visitor);

        assertFalse(Files.exists(file));
        assertFalse(Files.exists(tempDir));
    }

    @Test
    public void postVisitDirectory_shouldDeleteEmptyDirectory(@TempDir Path tempDir) throws IOException {
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectory(subDir);

        AtomicInteger deletedCount = new AtomicInteger(0);

        DeleteTreeVisitor visitor = new DeleteTreeVisitor(deletedCount, null);

        assertTrue(Files.exists(subDir));

        Files.walkFileTree(tempDir, visitor);

        assertFalse(Files.exists(subDir));
        assertFalse(Files.exists(tempDir));
        assertEquals(2, deletedCount.get());
    }

    @Test
    public void walkFileTree_withNestedStructure_shouldTrackAllDeletions(@TempDir Path tempDir) throws IOException {
        Path subDir1 = tempDir.resolve("sub1");
        Path subDir2 = tempDir.resolve("sub2");
        Files.createDirectories(subDir1);
        Files.createDirectories(subDir2);

        Files.writeString(subDir1.resolve("file1.txt"), "content1");
        Files.writeString(subDir1.resolve("file2.txt"), "content2");
        Files.writeString(subDir2.resolve("file3.txt"), "content3");

        AtomicInteger deletedCount = new AtomicInteger(0);
        AtomicLong freedSpace = new AtomicLong(0);

        DeleteTreeVisitor visitor = new DeleteTreeVisitor(deletedCount, freedSpace);

        Files.walkFileTree(tempDir, visitor);

        assertFalse(Files.exists(tempDir));
        assertEquals(6, deletedCount.get());
        assertTrue(freedSpace.get() > 0);
    }

    @Test
    public void visitFile_shouldTrackFileSize(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("sized-file.txt");
        String content = "This is test content with known size";
        Files.writeString(file, content);

        AtomicLong freedSpace = new AtomicLong(0);

        DeleteTreeVisitor visitor = new DeleteTreeVisitor(null, freedSpace);

        Files.walkFileTree(file, visitor);

        assertTrue(freedSpace.get() >= content.length());
    }

    @Test
    public void walkFileTree_withOnlyDirectories_shouldDeleteAll(@TempDir Path tempDir) throws IOException {
        Path level1 = tempDir.resolve("level1");
        Path level2 = level1.resolve("level2");
        Path level3 = level2.resolve("level3");
        Files.createDirectories(level3);

        AtomicInteger deletedCount = new AtomicInteger(0);

        DeleteTreeVisitor visitor = new DeleteTreeVisitor(deletedCount, null);

        Files.walkFileTree(tempDir, visitor);

        assertFalse(Files.exists(tempDir));
        assertEquals(4, deletedCount.get());
    }

    @Test
    public void walkFileTree_withMixedEmptyAndNonEmptyDirs_shouldDeleteAll(@TempDir Path tempDir) throws IOException {
        Path emptyDir = tempDir.resolve("empty");
        Path nonEmptyDir = tempDir.resolve("non-empty");
        Files.createDirectories(emptyDir);
        Files.createDirectories(nonEmptyDir);
        Files.writeString(nonEmptyDir.resolve("file.txt"), "content");

        AtomicInteger deletedCount = new AtomicInteger(0);

        DeleteTreeVisitor visitor = new DeleteTreeVisitor(deletedCount, null);

        Files.walkFileTree(tempDir, visitor);

        assertFalse(Files.exists(tempDir));
        assertEquals(4, deletedCount.get());
    }
}
