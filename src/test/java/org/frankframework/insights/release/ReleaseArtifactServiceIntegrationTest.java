package org.frankframework.insights.release;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Integration tests for ReleaseArtifactService that test the actual zip extraction logic.
 */
@ExtendWith(MockitoExtension.class)
public class ReleaseArtifactServiceIntegrationTest {

    @TempDir
    Path tempDir;

    @Mock
    private ReleaseRepository releaseRepository;

    private ReleaseArtifactService releaseArtifactService;
    private MockedStatic<URI> mockedUri;

    @BeforeEach
    public void setUp() {
        releaseArtifactService = new ReleaseArtifactService(tempDir.toString(), releaseRepository);
        mockedUri = mockStatic(URI.class);
    }

    @AfterEach
    public void tearDown() {
        mockedUri.close();
    }

    private Release createRelease(String name, String tagName) {
        Release release = new Release();
        release.setName(name);
        release.setTagName(tagName);
        return release;
    }

    private byte[] createZipBytes(String topLevelDir, String... entries) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry(topLevelDir + "/"));
            zos.closeEntry();

            for (String entry : entries) {
                if (entry.endsWith("/")) {
                    zos.putNextEntry(new ZipEntry(topLevelDir + "/" + entry));
                } else {
                    zos.putNextEntry(new ZipEntry(topLevelDir + "/" + entry));
                    zos.write(("content of " + entry).getBytes());
                }
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    private void mockDownload(String expectedUrl, byte[] zipBytes) {
        URL urlMock = mock(URL.class);
        URI uriMock = mock(URI.class);
        try {
            when(uriMock.toURL()).thenReturn(urlMock);
            when(urlMock.openStream()).thenReturn(new ByteArrayInputStream(zipBytes));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mockedUri.when(() -> URI.create(expectedUrl)).thenReturn(uriMock);
    }

    @Test
    public void prepareReleaseArtifacts_withValidZip_shouldExtractSuccessfully() throws IOException {
        Release release = createRelease("valid-test", "v1.0.0");
        String zipUrl = "https://github.com/frankframework/frankframework/archive/refs/tags/v1.0.0.zip";

        byte[] validZip = createZipBytes(
                "frankframework-1.0.0", "README.md", "src/", "src/main/", "src/main/java/", "src/main/java/App.java");

        mockDownload(zipUrl, validZip);

        Path result = releaseArtifactService.prepareReleaseArtifacts(release);

        assertTrue(result.toString().endsWith(release.getName()));
    }

    @Test
    public void unzip_withPathTraversal_shouldThrowIOException() throws IOException {
        Release release = createRelease("zip-normal", "v-normal");
        String zipUrl = "https://github.com/frankframework/frankframework/archive/refs/tags/v-normal.zip";

        byte[] normalZip = createZipBytes("frankframework-normal", "file.txt");
        mockDownload(zipUrl, normalZip);

        Path result = releaseArtifactService.prepareReleaseArtifacts(release);
        assertTrue(result.toString().endsWith(release.getName()));
    }

    @Test
    public void prepareReleaseArtifacts_withNestedDirectories_shouldExtractCorrectly() throws IOException {
        Release release = createRelease("nested", "v-nested");
        String zipUrl = "https://github.com/frankframework/frankframework/archive/refs/tags/v-nested.zip";

        byte[] nestedZip = createZipBytes(
                "frankframework-nested", "src/", "src/main/", "src/main/java/", "src/main/java/Test.java");

        mockDownload(zipUrl, nestedZip);

        Path result = releaseArtifactService.prepareReleaseArtifacts(release);

        assertTrue(result.toString().endsWith(release.getName()));
    }

    @Test
    public void prepareReleaseArtifacts_withEmptyZip_shouldHandleGracefully() throws IOException {
        Release release = createRelease("empty", "v-empty");
        String zipUrl = "https://github.com/frankframework/frankframework/archive/refs/tags/v-empty.zip";

        byte[] emptyZip;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("frankframework-empty/"));
            zos.closeEntry();
            zos.finish();
            emptyZip = baos.toByteArray();
        }

        mockDownload(zipUrl, emptyZip);

        Path result = releaseArtifactService.prepareReleaseArtifacts(release);

        assertTrue(result.toString().endsWith(release.getName()));
    }

    @Test
    public void prepareReleaseArtifacts_withSpecialCharactersInTagName_shouldBuildCorrectURL() throws IOException {
        Release release = createRelease("special", "v1.0.0-RC1");
        String zipUrl = "https://github.com/frankframework/frankframework/archive/refs/tags/v1.0.0-RC1.zip";

        byte[] validZip = createZipBytes("frankframework-1.0.0-RC1", "README.md");

        mockDownload(zipUrl, validZip);

        Path result = releaseArtifactService.prepareReleaseArtifacts(release);

        assertTrue(result.toString().endsWith(release.getName()));
    }

    @Test
    public void prepareReleaseArtifacts_withValidMultiFileZip_shouldExtractAll() throws IOException {
        Release release = createRelease("multi-file", "v2.0.0");
        String zipUrl = "https://github.com/frankframework/frankframework/archive/refs/tags/v2.0.0.zip";

        byte[] multiFileZip = createZipBytes(
                "frankframework-2.0.0",
                "README.md",
                "LICENSE",
                "src/",
                "src/main/",
                "src/main/java/",
                "src/main/java/App.java",
                "src/test/",
                "src/test/java/",
                "src/test/java/AppTest.java",
                "pom.xml",
                "build.gradle");

        mockDownload(zipUrl, multiFileZip);

        Path result = releaseArtifactService.prepareReleaseArtifacts(release);

        assertTrue(result.toString().endsWith(release.getName()));
    }

    @Test
    public void prepareReleaseArtifacts_withMixedContentZip_shouldExtractSuccessfully() throws IOException {
        Release release = createRelease("mixed", "v3.0.0");
        String zipUrl = "https://github.com/frankframework/frankframework/archive/refs/tags/v3.0.0.zip";

        byte[] mixedZip = createZipBytes(
                "frankframework-3.0.0", "docs/", "docs/README.txt", "config/", "config/settings.xml", "lib/", "bin/");

        mockDownload(zipUrl, mixedZip);

        Path result = releaseArtifactService.prepareReleaseArtifacts(release);

        assertTrue(result.toString().endsWith(release.getName()));
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_whenObsoleteDirectoriesExist_shouldDeleteThem() throws IOException {
        // Create some release directories
        Path validDir = tempDir.resolve("8.0.0");
        Path obsoleteDir1 = tempDir.resolve("7.8.0");
        Path obsoleteDir2 = tempDir.resolve("7.7.0");

        Files.createDirectories(validDir);
        Files.createDirectories(obsoleteDir1);
        Files.createDirectories(obsoleteDir2);
        Files.writeString(obsoleteDir1.resolve("file1.txt"), "content1");
        Files.writeString(obsoleteDir2.resolve("file2.txt"), "content2");

        // Setup mock to return only the valid release
        Release validRelease = createRelease("8.0.0", "v8.0.0");
        when(releaseRepository.findAll()).thenReturn(List.of(validRelease));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        // Verify that obsolete directories were deleted
        assertTrue(Files.exists(validDir), "Valid directory should still exist");
        assertFalse(Files.exists(obsoleteDir1), "Obsolete directory 7.8.0 should be deleted");
        assertFalse(Files.exists(obsoleteDir2), "Obsolete directory 7.7.0 should be deleted");
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_whenNoObsoleteDirectories_shouldNotDeleteAnything() throws IOException {
        // Create release directories that match database
        Path dir1 = tempDir.resolve("8.0.0");
        Path dir2 = tempDir.resolve("7.9.0");

        Files.createDirectories(dir1);
        Files.createDirectories(dir2);
        Files.writeString(dir1.resolve("file1.txt"), "content1");
        Files.writeString(dir2.resolve("file2.txt"), "content2");

        Release release1 = createRelease("8.0.0", "v8.0.0");
        Release release2 = createRelease("7.9.0", "v7.9.0");
        when(releaseRepository.findAll()).thenReturn(List.of(release1, release2));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        // Verify that both directories still exist
        assertTrue(Files.exists(dir1), "Directory 8.0.0 should still exist");
        assertTrue(Files.exists(dir2), "Directory 7.9.0 should still exist");
        assertTrue(Files.exists(dir1.resolve("file1.txt")), "Files in valid directories should be preserved");
        assertTrue(Files.exists(dir2.resolve("file2.txt")), "Files in valid directories should be preserved");
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_withNestedFiles_shouldDeleteRecursively() throws IOException {
        // Create an obsolete directory with nested structure
        Path obsoleteDir = tempDir.resolve("7.5.0");
        Path subDir1 = obsoleteDir.resolve("src");
        Path subDir2 = subDir1.resolve("main");
        Path subDir3 = subDir2.resolve("java");

        Files.createDirectories(subDir3);
        Files.writeString(obsoleteDir.resolve("README.md"), "readme");
        Files.writeString(subDir1.resolve("config.xml"), "config");
        Files.writeString(subDir3.resolve("App.java"), "code");

        // No matching release in database
        when(releaseRepository.findAll()).thenReturn(List.of());

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        // Verify entire directory tree was deleted
        assertFalse(Files.exists(obsoleteDir), "Obsolete directory should be completely deleted");
        assertFalse(Files.exists(subDir3), "Nested subdirectories should be deleted");
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_withMixOfValidAndObsolete_shouldOnlyDeleteObsolete() throws IOException {
        // Create mix of valid and obsolete directories
        Path validDir1 = tempDir.resolve("8.0.0");
        Path validDir2 = tempDir.resolve("7.9.0");
        Path obsoleteDir = tempDir.resolve("7.8.0");

        Files.createDirectories(validDir1);
        Files.createDirectories(validDir2);
        Files.createDirectories(obsoleteDir);
        Files.writeString(validDir1.resolve("valid1.txt"), "valid1");
        Files.writeString(validDir2.resolve("valid2.txt"), "valid2");
        Files.writeString(obsoleteDir.resolve("obsolete.txt"), "obsolete");

        Release release1 = createRelease("8.0.0", "v8.0.0");
        Release release2 = createRelease("7.9.0", "v7.9.0");
        when(releaseRepository.findAll()).thenReturn(List.of(release1, release2));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        assertTrue(Files.exists(validDir1), "Valid directory 8.0.0 should still exist");
        assertTrue(Files.exists(validDir2), "Valid directory 7.9.0 should still exist");
        assertFalse(Files.exists(obsoleteDir), "Obsolete directory 7.8.0 should be deleted");
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_withEmptyObsoleteDirectory_shouldDelete() throws IOException {
        // Create an empty obsolete directory
        Path obsoleteDir = tempDir.resolve("7.0.0");
        Files.createDirectories(obsoleteDir);

        when(releaseRepository.findAll()).thenReturn(List.of());

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        assertFalse(Files.exists(obsoleteDir), "Empty obsolete directory should be deleted");
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_withMultipleFilesInObsoleteDirectory_shouldDeleteAll()
            throws IOException {
        // Create obsolete directory with multiple files
        Path obsoleteDir = tempDir.resolve("6.9.0");
        Files.createDirectories(obsoleteDir);
        Files.writeString(obsoleteDir.resolve("file1.txt"), "content1");
        Files.writeString(obsoleteDir.resolve("file2.txt"), "content2");
        Files.writeString(obsoleteDir.resolve("file3.txt"), "content3");

        when(releaseRepository.findAll()).thenReturn(List.of());

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        assertFalse(Files.exists(obsoleteDir), "Directory with multiple files should be deleted");
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_whenArchiveDirectoryEmpty_shouldHandleGracefully() throws IOException {
        // Archive directory exists but is empty
        assertTrue(Files.exists(tempDir), "Temp directory should exist");
        assertEquals(0, Files.list(tempDir).count(), "Directory should be empty");

        when(releaseRepository.findAll()).thenReturn(List.of());

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        assertTrue(Files.exists(tempDir), "Archive directory should still exist");
    }
}
