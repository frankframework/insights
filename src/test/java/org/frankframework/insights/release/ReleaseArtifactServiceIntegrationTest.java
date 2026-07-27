package org.frankframework.insights.release;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Integration tests for ReleaseArtifactService that test the actual file operations.
 */
@ExtendWith(MockitoExtension.class)
public class ReleaseArtifactServiceIntegrationTest {

    @TempDir
    Path tempDir;

    @Mock
    private ReleaseRepository releaseRepository;

    private ReleaseArtifactService releaseArtifactService;

    @BeforeEach
    public void setUp() {
        releaseArtifactService = Mockito.spy(new ReleaseArtifactService(tempDir.toString(), releaseRepository));
    }

    private Release createRelease(String name, String tagName) {
        Release release = new Release();
        release.setName(name);
        release.setTagName(tagName);
        return release;
    }

    private byte[] createValidZipBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("test.txt"));
            zos.write("hello world".getBytes());
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    @Test
    public void downloadReleaseZipToPvc_whenCorruptFileInCache_shouldRedownloadAndReplace() throws Exception {
        String tagName = "v8.0.5";
        Path corruptZip = tempDir.resolve("v8.0.5.zip");
        Files.writeString(corruptZip, "this is not a zip file");

        InputStream mockStream = new ByteArrayInputStream(createValidZipBytes());
        doReturn(mockStream).when(releaseArtifactService).openDownloadStream(any());

        Path downloaded = releaseArtifactService.downloadReleaseZipToPvc(tagName);

        assertTrue(Files.exists(downloaded), "Downloaded zip should exist");
        assertTrue(releaseArtifactService.isValidZip(downloaded), "Re-downloaded zip should be valid");
        assertFalse(
                Files.exists(tempDir.resolve("v8.0.5.zip.tmp")), "Temp file should not remain after a successful move");
    }

    @Test
    public void downloadReleaseZipToPvc_whenDownloadIsCorrupt_shouldThrowAndCleanUpTempFile() throws Exception {
        String tagName = "v8.0.6";
        Path zip = tempDir.resolve("v8.0.6.zip");
        Path tempZip = tempDir.resolve("v8.0.6.zip.tmp");

        InputStream corruptStream = new ByteArrayInputStream("bad download".getBytes());
        doReturn(corruptStream).when(releaseArtifactService).openDownloadStream(any());

        assertThrows(IOException.class, () -> releaseArtifactService.downloadReleaseZipToPvc(tagName));

        assertFalse(Files.exists(tempZip), "Temporary download file should be cleaned up on failure");
        assertFalse(Files.exists(zip), "Invalid download should never be cached under its final name");
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_whenOrphanedTempFilesExist_shouldDeleteThem() throws IOException {
        Path orphanedTemp = tempDir.resolve("v8.0.5.zip.tmp");
        Path validZip = tempDir.resolve("v8.0.0.zip");

        Files.writeString(orphanedTemp, "leftover temp");
        Files.writeString(validZip, "valid zip content");

        Release validRelease = createRelease("8.0.0", "v8.0.0");
        when(releaseRepository.findAll()).thenReturn(List.of(validRelease));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        assertFalse(Files.exists(orphanedTemp), "Orphaned temp file should be deleted");
        assertTrue(Files.exists(validZip), "Valid active zip should still exist");
    }

    @Test
    public void downloadReleaseZipToPvc_whenValidZipAlreadyCached_shouldReturnItWithoutDownloading()
            throws IOException, URISyntaxException {
        String tagName = "v8.1.0";
        Path zipPath = tempDir.resolve("v8.1.0.zip");
        Files.write(zipPath, createValidZipBytes());

        Path result = releaseArtifactService.downloadReleaseZipToPvc(tagName);

        assertEquals(zipPath, result);
        Mockito.verify(releaseArtifactService, Mockito.never()).openDownloadStream(any());
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_whenObsoleteZipsExist_shouldDeleteThem() throws IOException {
        Path validZip = tempDir.resolve("v8.0.0.zip");
        Path obsoleteZip1 = tempDir.resolve("v7.8.0.zip");
        Path obsoleteZip2 = tempDir.resolve("v7.7.0.zip");

        Files.writeString(validZip, "valid zip content");
        Files.writeString(obsoleteZip1, "obsolete zip 1");
        Files.writeString(obsoleteZip2, "obsolete zip 2");

        Release validRelease = createRelease("8.0.0", "v8.0.0");
        when(releaseRepository.findAll()).thenReturn(List.of(validRelease));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        assertTrue(Files.exists(validZip), "Valid zip should still exist");
        assertFalse(Files.exists(obsoleteZip1), "Obsolete zip v7.8.0 should be deleted");
        assertFalse(Files.exists(obsoleteZip2), "Obsolete zip v7.7.0 should be deleted");
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_whenNoObsoleteZips_shouldNotDeleteAnything() throws IOException {
        Path zip1 = tempDir.resolve("v8.0.0.zip");
        Path zip2 = tempDir.resolve("v7.9.0.zip");

        Files.writeString(zip1, "zip content 1");
        Files.writeString(zip2, "zip content 2");

        Release release1 = createRelease("8.0.0", "v8.0.0");
        Release release2 = createRelease("7.9.0", "v7.9.0");
        when(releaseRepository.findAll()).thenReturn(List.of(release1, release2));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        assertTrue(Files.exists(zip1), "Zip v8.0.0 should still exist");
        assertTrue(Files.exists(zip2), "Zip v7.9.0 should still exist");
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_withMixOfValidAndObsolete_shouldOnlyDeleteObsolete() throws IOException {
        Path validZip1 = tempDir.resolve("v8.0.0.zip");
        Path validZip2 = tempDir.resolve("v7.9.0.zip");
        Path obsoleteZip = tempDir.resolve("v7.8.0.zip");

        Files.writeString(validZip1, "valid1");
        Files.writeString(validZip2, "valid2");
        Files.writeString(obsoleteZip, "obsolete");

        Release release1 = createRelease("8.0.0", "v8.0.0");
        Release release2 = createRelease("7.9.0", "v7.9.0");
        when(releaseRepository.findAll()).thenReturn(List.of(release1, release2));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        assertTrue(Files.exists(validZip1), "Valid zip v8.0.0 should still exist");
        assertTrue(Files.exists(validZip2), "Valid zip v7.9.0 should still exist");
        assertFalse(Files.exists(obsoleteZip), "Obsolete zip v7.8.0 should be deleted");
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_withEmptyObsoleteZip_shouldDelete() throws IOException {
        Path obsoleteZip = tempDir.resolve("v7.0.0.zip");
        Files.createFile(obsoleteZip);

        when(releaseRepository.findAll()).thenReturn(List.of());

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        assertFalse(Files.exists(obsoleteZip), "Empty obsolete zip should be deleted");
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_whenArchiveDirectoryEmpty_shouldHandleGracefully() throws IOException {
        assertTrue(Files.exists(tempDir), "Temp directory should exist");
        assertEquals(0, Files.list(tempDir).count(), "Directory should be empty");

        when(releaseRepository.findAll()).thenReturn(List.of());

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        assertTrue(Files.exists(tempDir), "Archive directory should still exist");
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_shouldIgnoreNonZipFiles() throws IOException {
        Path validZip = tempDir.resolve("v8.0.0.zip");
        Path txtFile = tempDir.resolve("readme.txt");
        Path subDir = tempDir.resolve("some-folder");

        Files.writeString(validZip, "valid zip");
        Files.writeString(txtFile, "readme content");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("nested.txt"), "nested");

        Release release = createRelease("8.0.0", "v8.0.0");
        when(releaseRepository.findAll()).thenReturn(List.of(release));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        assertTrue(Files.exists(validZip), "Valid zip should still exist");
        assertTrue(Files.exists(txtFile), "Non-zip file should not be touched");
        assertTrue(Files.exists(subDir), "Directory should not be touched");
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_withAllObsolete_shouldDeleteAll() throws IOException {
        Path obsoleteZip1 = tempDir.resolve("v1.0.0.zip");
        Path obsoleteZip2 = tempDir.resolve("v2.0.0.zip");
        Path obsoleteZip3 = tempDir.resolve("v3.0.0.zip");

        Files.writeString(obsoleteZip1, "content1");
        Files.writeString(obsoleteZip2, "content2");
        Files.writeString(obsoleteZip3, "content3");

        when(releaseRepository.findAll()).thenReturn(List.of());

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        assertFalse(Files.exists(obsoleteZip1), "Obsolete zip v1.0.0 should be deleted");
        assertFalse(Files.exists(obsoleteZip2), "Obsolete zip v2.0.0 should be deleted");
        assertFalse(Files.exists(obsoleteZip3), "Obsolete zip v3.0.0 should be deleted");
    }
}
