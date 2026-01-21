package org.frankframework.insights.release.releasecleanup;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class FileTreeDeleterTest {

    private FileTreeDeleter fileTreeDeleter;

    @BeforeEach
    public void setUp() {
        fileTreeDeleter = new FileTreeDeleter();
    }

    @Test
    public void deleteTreeRecursively_withNullPath_shouldNotThrow() {
        assertDoesNotThrow(() -> fileTreeDeleter.deleteTreeRecursively(null));
    }

    @Test
    public void deleteTreeRecursively_withNonExistentPath_shouldNotThrow(@TempDir Path tempDir) {
        Path nonExistent = tempDir.resolve("does-not-exist");

        assertDoesNotThrow(() -> fileTreeDeleter.deleteTreeRecursively(nonExistent));
    }

    @Test
    public void deleteTreeRecursively_withEmptyDirectory_shouldDeleteIt(@TempDir Path tempDir)
            throws IOException, InterruptedException {
        Path emptyDir = tempDir.resolve("empty-dir");
        Files.createDirectory(emptyDir);

        assertTrue(Files.exists(emptyDir));

        fileTreeDeleter.deleteTreeRecursively(emptyDir);

        assertFalse(Files.exists(emptyDir));
    }

    @Test
    public void deleteTreeRecursively_withSingleFile_shouldDeleteIt(@TempDir Path tempDir)
            throws IOException, InterruptedException {
        Path singleFile = tempDir.resolve("single-file.txt");
        Files.writeString(singleFile, "test content");

        assertTrue(Files.exists(singleFile));

        fileTreeDeleter.deleteTreeRecursively(singleFile);

        assertFalse(Files.exists(singleFile));
    }

    @Test
    public void deleteTreeRecursively_withNestedDirectories_shouldDeleteAll(@TempDir Path tempDir)
            throws IOException, InterruptedException {
        Path rootDir = tempDir.resolve("root");
        Path subDir1 = rootDir.resolve("sub1");
        Path subDir2 = rootDir.resolve("sub2");
        Path deepDir = subDir1.resolve("deep");

        Files.createDirectories(deepDir);
        Files.createDirectories(subDir2);

        Files.writeString(rootDir.resolve("file1.txt"), "content1");
        Files.writeString(subDir1.resolve("file2.txt"), "content2");
        Files.writeString(subDir2.resolve("file3.txt"), "content3");
        Files.writeString(deepDir.resolve("file4.txt"), "content4");

        assertTrue(Files.exists(rootDir));
        assertTrue(Files.exists(deepDir.resolve("file4.txt")));

        fileTreeDeleter.deleteTreeRecursively(rootDir);

        assertFalse(Files.exists(rootDir));
        assertFalse(Files.exists(subDir1));
        assertFalse(Files.exists(subDir2));
        assertFalse(Files.exists(deepDir));
    }

    @Test
    public void deleteTreeRecursively_withMixedContent_shouldDeleteAll(@TempDir Path tempDir)
            throws IOException, InterruptedException {
        Path rootDir = tempDir.resolve("mixed");
        Files.createDirectories(rootDir);

        Files.writeString(rootDir.resolve("text.txt"), "text");
        Files.writeString(rootDir.resolve("data.json"), "{}");
        Files.writeString(rootDir.resolve("config.xml"), "<config/>");

        Path subDir = rootDir.resolve("subdir");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("nested.txt"), "nested");

        assertEquals(4, countFiles(rootDir));

        fileTreeDeleter.deleteTreeRecursively(rootDir);

        assertFalse(Files.exists(rootDir));
    }

    @Test
    public void deleteTreeRecursively_withRelativePath_shouldNormalizeAndDelete(@TempDir Path tempDir)
            throws IOException, InterruptedException {
        Path dir = tempDir.resolve("to-delete");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("file.txt"), "content");

        Path relativePath = tempDir.resolve("./to-delete/../to-delete");

        assertTrue(Files.exists(dir));

        fileTreeDeleter.deleteTreeRecursively(relativePath);

        assertFalse(Files.exists(dir));
    }

    @Test
    public void deleteTreeRecursively_withLargeNumberOfFiles_shouldDeleteAll(@TempDir Path tempDir)
            throws IOException, InterruptedException {
        Path rootDir = tempDir.resolve("many-files");
        Files.createDirectories(rootDir);

        for (int i = 0; i < 100; i++) {
            Files.writeString(rootDir.resolve("file" + i + ".txt"), "content" + i);
        }

        assertEquals(100, countFiles(rootDir));

        fileTreeDeleter.deleteTreeRecursively(rootDir);

        assertFalse(Files.exists(rootDir));
    }

    @Test
    public void deleteTreeRecursively_withDeeplyNestedStructure_shouldDeleteAll(@TempDir Path tempDir)
            throws IOException, InterruptedException {
        Path current = tempDir.resolve("deep");

        for (int i = 0; i < 10; i++) {
            current = current.resolve("level" + i);
        }
        Files.createDirectories(current);
        Files.writeString(current.resolve("deepfile.txt"), "deep content");

        Path rootDir = tempDir.resolve("deep");
        assertTrue(Files.exists(rootDir));

        fileTreeDeleter.deleteTreeRecursively(rootDir);

        assertFalse(Files.exists(rootDir));
    }

    @Test
    public void deleteTreeRecursively_withEmptyFiles_shouldDeleteAll(@TempDir Path tempDir)
            throws IOException, InterruptedException {
        Path rootDir = tempDir.resolve("empty-files");
        Files.createDirectories(rootDir);

        Files.createFile(rootDir.resolve("empty1.txt"));
        Files.createFile(rootDir.resolve("empty2.txt"));
        Files.createFile(rootDir.resolve("empty3.txt"));

        fileTreeDeleter.deleteTreeRecursively(rootDir);

        assertFalse(Files.exists(rootDir));
    }

    @Test
    public void deleteTreeRecursively_multipleCalls_shouldBeIdempotent(@TempDir Path tempDir)
            throws IOException, InterruptedException {
        Path dir = tempDir.resolve("idempotent-test");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("file.txt"), "content");

        fileTreeDeleter.deleteTreeRecursively(dir);
        assertFalse(Files.exists(dir));

        assertDoesNotThrow(() -> fileTreeDeleter.deleteTreeRecursively(dir));
    }

    private long countFiles(Path dir) throws IOException {
        try (var stream = Files.walk(dir)) {
            return stream.filter(Files::isRegularFile).count();
        }
    }
}
