package org.frankframework.insights.release;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private static final Path ARCHIVE_DIR = Paths.get("/release-archive");

    @BeforeEach
    public void setUp() {
        releaseArtifactService = new ReleaseArtifactService("/release-archive", releaseRepository);
        mockedFiles = Mockito.mockStatic(Files.class);
    }

    @AfterEach
    public void tearDown() {
        mockedFiles.close();
    }

    private Release createRelease(String name, String tagName) {
        Release release = new Release();
        release.setName(name);
        release.setTagName(tagName);
        return release;
    }

    @Test
    public void downloadReleaseZipToPvc_whenZipAlreadyExists_shouldReturnExistingPath() throws IOException {
        String tagName = "v7.8.0";
        Path zipPath = ARCHIVE_DIR.resolve(tagName + ".zip");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        mockedFiles.when(() -> Files.exists(zipPath)).thenReturn(true);

        Path result = releaseArtifactService.downloadReleaseZipToPvc(tagName);

        assertEquals(zipPath, result);
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_whenArchiveDirectoryDoesNotExist_shouldSkipCleanup() {
        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(false);

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.list(any()), never());
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_whenNoObsoleteZips_shouldNotDeleteAnything() throws IOException {
        Release release1 = createRelease("7.8.0", "v7.8.0");
        Release release2 = createRelease("8.0.0", "v8.0.0");
        List<Release> releases = List.of(release1, release2);

        Path zip1 = ARCHIVE_DIR.resolve("v7.8.0.zip");
        Path zip2 = ARCHIVE_DIR.resolve("v8.0.0.zip");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        when(releaseRepository.findAll()).thenReturn(releases);
        mockedFiles.when(() -> Files.list(ARCHIVE_DIR)).thenReturn(Stream.of(zip1, zip2));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.walkFileTree(any(), any()), never());
        mockedFiles.verify(() -> Files.delete(any()), never());
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_whenListDirectoriesFails_shouldHandleGracefully() throws IOException {
        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        when(releaseRepository.findAll()).thenReturn(new ArrayList<>());
        mockedFiles.when(() -> Files.list(ARCHIVE_DIR)).thenThrow(new IOException("Permission denied"));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.walkFileTree(any(), any()), never());
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_shouldIgnoreNonZipFiles() throws IOException {
        Release release1 = createRelease("8.0.0", "v8.0.0");
        List<Release> releases = List.of(release1);

        Path validZip = ARCHIVE_DIR.resolve("v8.0.0.zip");
        Path txtFile = ARCHIVE_DIR.resolve("readme.txt");
        Path directory = ARCHIVE_DIR.resolve("some-folder");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        when(releaseRepository.findAll()).thenReturn(releases);
        mockedFiles.when(() -> Files.list(ARCHIVE_DIR)).thenReturn(Stream.of(validZip, txtFile, directory));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.walkFileTree(any(), any()), never());
    }
}
