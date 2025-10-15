package org.frankframework.insights.release;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReleaseArtifactServiceTest {

    private ReleaseArtifactService releaseArtifactService;

    private MockedStatic<Files> mockedFiles;
    private MockedStatic<URI> mockedUri;
    private static final Path ARCHIVE_DIR = Paths.get("./release-archive");

    @BeforeEach
    public void setUp() throws Exception {
        releaseArtifactService = new ReleaseArtifactService();

        Field field = ReleaseArtifactService.class.getDeclaredField("archiveDirectory");
        field.setAccessible(true);
        field.set(releaseArtifactService, ARCHIVE_DIR.toString());

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

    private void mockDownload(String expectedUrl, byte[] zipBytes) throws IOException {
        URL urlMock = mock(URL.class);
        URI uriMock = mock(URI.class);
        when(uriMock.toURL()).thenReturn(urlMock);
        mockedUri.when(() -> URI.create(expectedUrl)).thenReturn(uriMock);
        when(urlMock.openStream()).thenReturn(new ByteArrayInputStream(zipBytes));
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
}
