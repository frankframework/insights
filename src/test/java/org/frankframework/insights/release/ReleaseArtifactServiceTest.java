package org.frankframework.insights.release;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReleaseArtifactServiceTest {

    private ReleaseArtifactService releaseArtifactService;

    @Mock
    private ReleaseRepository releaseRepository;

    private MockedStatic<Files> mockedFiles;
    private MockedStatic<URI> mockedUri;
    private static final Path ARCHIVE_DIR = Paths.get("/release-archive");

    @BeforeEach
    public void setUp() throws Exception {
        releaseArtifactService = new ReleaseArtifactService("/release-archive", releaseRepository);

        mockedFiles = Mockito.mockStatic(Files.class);
        mockedUri = Mockito.mockStatic(URI.class);
    }

    @AfterEach
    public void tearDown() {
        mockedFiles.close();
        mockedUri.close();
    }

    private Release createRelease(String name, String tagName) {
        Release release = new Release();
        release.setName(name);
        release.setTagName(tagName);
        return release;
    }

    @Test
    public void prepareReleaseArtifacts_whenDirectoryExistsAndIsNotEmpty_shouldSkipDownload() throws IOException {
        Release release = createRelease("7.8.0", "v7.8.0");
        Path releaseDir = ARCHIVE_DIR.resolve(release.getName());

        mockedFiles.when(() -> Files.isDirectory(releaseDir)).thenReturn(true);
        mockedFiles.when(() -> Files.list(releaseDir)).thenReturn(Stream.of(releaseDir.resolve("somefile.txt")));

        Path result = releaseArtifactService.prepareReleaseArtifacts(release);

        assertEquals(releaseDir, result);
        mockedFiles.verify(() -> Files.createDirectories(any()), never());
        mockedFiles.verify(
                () -> Files.copy(any(InputStream.class), any(Path.class), any(StandardCopyOption.class)), never());
    }

    @Test
    public void prepareReleaseArtifacts_whenReleaseHasNoTagName_shouldThrowIOException() {
        Release release = createRelease("snapshot-2023", null);
        Exception e = assertThrows(IOException.class, () -> releaseArtifactService.prepareReleaseArtifacts(release));
        assertTrue(e.getMessage().contains("is missing a tagName"));
    }

    @Test
    public void prepareReleaseArtifacts_whenDownloadFails_shouldThrowIOException() throws IOException {
        Release release = createRelease("8.0.0", "v8.0.0");
        Path releaseDir = ARCHIVE_DIR.resolve(release.getName());
        String zipUrl = "https://github.com/frankframework/frankframework/archive/refs/tags/v8.0.0.zip";

        mockedFiles.when(() -> Files.isDirectory(releaseDir)).thenReturn(false);
        mockedFiles.when(() -> Files.createDirectories(releaseDir)).thenReturn(releaseDir);

        URL urlMock = mock(URL.class);
        URI uriMock = mock(URI.class);
        when(uriMock.toURL()).thenReturn(urlMock);
        mockedUri.when(() -> URI.create(zipUrl)).thenReturn(uriMock);
        when(urlMock.openStream()).thenThrow(new IOException("404 Not Found"));

        assertThrows(IOException.class, () -> releaseArtifactService.prepareReleaseArtifacts(release));
    }

    @Test
    public void prepareReleaseArtifacts_whenDirectoryExists_shouldSkipDownload() throws IOException {
        Release release = createRelease("7.8.0", "v7.8.0");
        Path releaseDir = ARCHIVE_DIR.resolve(release.getName());

        mockedFiles.when(() -> Files.isDirectory(eq(releaseDir))).thenReturn(true);
        mockedFiles.when(() -> Files.list(eq(releaseDir))).thenReturn(Stream.of(releaseDir.resolve("somefile.txt")));

        Path result = releaseArtifactService.prepareReleaseArtifacts(release);

        assertEquals(releaseDir, result);
        mockedUri.verify(() -> URI.create(any()), never());
    }

    @Test
    public void prepareReleaseArtifacts_withBlankTagName_shouldThrowIOException() {
        Release release = createRelease("blank-tag", "   ");
        Exception e = assertThrows(IOException.class, () -> releaseArtifactService.prepareReleaseArtifacts(release));
        assertTrue(e.getMessage().contains("is missing a tagName"));
    }

    @Test
    public void prepareReleaseArtifacts_whenURICreationFails_shouldThrowException() {
        Release release = createRelease("bad-uri", "v bad uri");
        mockedFiles.when(() -> Files.isDirectory(any())).thenReturn(false);

        String zipUrl = "https://github.com/frankframework/frankframework/archive/refs/tags/v bad uri.zip";
        mockedUri.when(() -> URI.create(zipUrl)).thenThrow(new IllegalArgumentException("Invalid URI"));

        assertThrows(IllegalArgumentException.class, () -> releaseArtifactService.prepareReleaseArtifacts(release));
    }

    @Test
    public void prepareReleaseArtifacts_whenURLConversionFails_shouldThrowException() throws IOException {
        Release release = createRelease("bad-url", "v-bad-url");
        Path releaseDir = ARCHIVE_DIR.resolve(release.getName());
        mockedFiles.when(() -> Files.isDirectory(releaseDir)).thenReturn(false);
        mockedFiles.when(() -> Files.createDirectories(releaseDir)).thenReturn(releaseDir);

        String zipUrl = "https://github.com/frankframework/frankframework/archive/refs/tags/v-bad-url.zip";
        URI uriMock = mock(URI.class);
        mockedUri.when(() -> URI.create(zipUrl)).thenReturn(uriMock);
        when(uriMock.toURL()).thenThrow(new MalformedURLException("Bad URL"));

        assertThrows(MalformedURLException.class, () -> releaseArtifactService.prepareReleaseArtifacts(release));
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_whenArchiveDirectoryDoesNotExist_shouldSkipCleanup() {
        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(false);

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.list(any()), never());
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_whenNoObsoleteDirectories_shouldNotDeleteAnything() {
        Release release1 = createRelease("7.8.0", "v7.8.0");
        Release release2 = createRelease("8.0.0", "v8.0.0");
        List<Release> releases = List.of(release1, release2);

        Path dir1 = ARCHIVE_DIR.resolve("7.8.0");
        Path dir2 = ARCHIVE_DIR.resolve("8.0.0");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        when(releaseRepository.findAll()).thenReturn(releases);
        mockedFiles.when(() -> Files.list(ARCHIVE_DIR)).thenReturn(Stream.of(dir1, dir2));
        mockedFiles.when(() -> Files.isDirectory(dir1)).thenReturn(true);
        mockedFiles.when(() -> Files.isDirectory(dir2)).thenReturn(true);

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.walk(any()), never());
        mockedFiles.verify(() -> Files.delete(any()), never());
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_whenObsoleteDirectoriesExist_shouldDeleteThem() {
        Release release1 = createRelease("8.0.0", "v8.0.0");
        List<Release> releases = List.of(release1);

        Path validDir = ARCHIVE_DIR.resolve("8.0.0");
        Path obsoleteDir1 = ARCHIVE_DIR.resolve("7.8.0");
        Path obsoleteDir2 = ARCHIVE_DIR.resolve("7.7.0");
        Path obsoleteFile1 = obsoleteDir1.resolve("file1.txt");
        Path obsoleteFile2 = obsoleteDir2.resolve("file2.txt");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        when(releaseRepository.findAll()).thenReturn(releases);
        mockedFiles.when(() -> Files.list(ARCHIVE_DIR)).thenReturn(Stream.of(validDir, obsoleteDir1, obsoleteDir2));
        mockedFiles.when(() -> Files.isDirectory(validDir)).thenReturn(true);
        mockedFiles.when(() -> Files.isDirectory(obsoleteDir1)).thenReturn(true);
        mockedFiles.when(() -> Files.isDirectory(obsoleteDir2)).thenReturn(true);

        mockedFiles.when(() -> Files.exists(obsoleteDir1)).thenReturn(true);
        mockedFiles.when(() -> Files.exists(obsoleteDir2)).thenReturn(true);
        mockedFiles.when(() -> Files.walk(obsoleteDir1)).thenReturn(Stream.of(obsoleteDir1, obsoleteFile1));
        mockedFiles.when(() -> Files.walk(obsoleteDir2)).thenReturn(Stream.of(obsoleteDir2, obsoleteFile2));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.walk(obsoleteDir1));
        mockedFiles.verify(() -> Files.walk(obsoleteDir2));
        mockedFiles.verify(() -> Files.walk(validDir), never());
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_withMixOfValidAndObsolete_shouldOnlyDeleteObsolete() {
        Release release1 = createRelease("8.0.0", "v8.0.0");
        Release release2 = createRelease("7.9.0", "v7.9.0");
        List<Release> releases = List.of(release1, release2);

        Path validDir1 = ARCHIVE_DIR.resolve("8.0.0");
        Path validDir2 = ARCHIVE_DIR.resolve("7.9.0");
        Path obsoleteDir = ARCHIVE_DIR.resolve("7.8.0");
        Path obsoleteFile = obsoleteDir.resolve("file.txt");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        when(releaseRepository.findAll()).thenReturn(releases);
        mockedFiles.when(() -> Files.list(ARCHIVE_DIR)).thenReturn(Stream.of(validDir1, validDir2, obsoleteDir));
        mockedFiles.when(() -> Files.isDirectory(validDir1)).thenReturn(true);
        mockedFiles.when(() -> Files.isDirectory(validDir2)).thenReturn(true);
        mockedFiles.when(() -> Files.isDirectory(obsoleteDir)).thenReturn(true);

        mockedFiles.when(() -> Files.exists(obsoleteDir)).thenReturn(true);
        mockedFiles.when(() -> Files.walk(obsoleteDir)).thenReturn(Stream.of(obsoleteDir, obsoleteFile));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.walk(obsoleteDir));
        mockedFiles.verify(() -> Files.walk(validDir1), never());
        mockedFiles.verify(() -> Files.walk(validDir2), never());
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_whenListDirectoriesFails_shouldHandleGracefully() {
        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        when(releaseRepository.findAll()).thenReturn(new ArrayList<>());
        mockedFiles.when(() -> Files.list(ARCHIVE_DIR)).thenThrow(new IOException("Permission denied"));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.walk(any()), never());
    }
}
