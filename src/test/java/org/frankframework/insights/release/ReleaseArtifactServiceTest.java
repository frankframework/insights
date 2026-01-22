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
import org.junit.jupiter.api.Assertions;
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

    @Test
    public void downloadReleaseZipToPvc_whenArchiveDirectoryDoesNotExist_shouldCreateIt() throws IOException {
        String tagName = "v9.0.0";
        Path zipPath = ARCHIVE_DIR.resolve(tagName + ".zip");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(false);
        mockedFiles.when(() -> Files.createDirectories(ARCHIVE_DIR)).thenReturn(ARCHIVE_DIR);
        mockedFiles.when(() -> Files.exists(zipPath)).thenReturn(true);

        Path result = releaseArtifactService.downloadReleaseZipToPvc(tagName);

        assertEquals(zipPath, result);
        mockedFiles.verify(() -> Files.createDirectories(ARCHIVE_DIR));
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_whenObsoleteZipExists_shouldDeleteIt() throws IOException {
        Release release1 = createRelease("8.0.0", "v8.0.0");
        List<Release> releases = List.of(release1);

        Path activeZip = ARCHIVE_DIR.resolve("v8.0.0.zip");
        Path obsoleteZip = ARCHIVE_DIR.resolve("v7.0.0.zip");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        when(releaseRepository.findAll()).thenReturn(releases);
        mockedFiles.when(() -> Files.list(ARCHIVE_DIR)).thenReturn(Stream.of(activeZip, obsoleteZip));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.walkFileTree(any(), any()), never());
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_whenAllZipsAreObsolete_shouldDeleteAll() throws IOException {
        List<Release> releases = new ArrayList<>();

        Path obsoleteZip1 = ARCHIVE_DIR.resolve("v6.0.0.zip");
        Path obsoleteZip2 = ARCHIVE_DIR.resolve("v7.0.0.zip");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        when(releaseRepository.findAll()).thenReturn(releases);
        mockedFiles.when(() -> Files.list(ARCHIVE_DIR)).thenReturn(Stream.of(obsoleteZip1, obsoleteZip2));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.list(ARCHIVE_DIR));
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_withMixedContent_shouldOnlyProcessZipFiles() throws IOException {
        Release release1 = createRelease("8.0.0", "v8.0.0");
        List<Release> releases = List.of(release1);

        Path activeZip = ARCHIVE_DIR.resolve("v8.0.0.zip");
        Path textFile = ARCHIVE_DIR.resolve("notes.txt");
        Path jsonFile = ARCHIVE_DIR.resolve("config.json");
        Path subDir = ARCHIVE_DIR.resolve("extracted");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        when(releaseRepository.findAll()).thenReturn(releases);
        mockedFiles.when(() -> Files.list(ARCHIVE_DIR)).thenReturn(Stream.of(activeZip, textFile, jsonFile, subDir));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.walkFileTree(any(), any()), never());
        mockedFiles.verify(() -> Files.delete(any()), never());
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_withMultipleActiveReleases_shouldKeepAllActive() throws IOException {
        Release release1 = createRelease("7.8.0", "v7.8.0");
        Release release2 = createRelease("8.0.0", "v8.0.0");
        Release release3 = createRelease("8.1.0", "v8.1.0");
        List<Release> releases = List.of(release1, release2, release3);

        Path zip1 = ARCHIVE_DIR.resolve("v7.8.0.zip");
        Path zip2 = ARCHIVE_DIR.resolve("v8.0.0.zip");
        Path zip3 = ARCHIVE_DIR.resolve("v8.1.0.zip");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        when(releaseRepository.findAll()).thenReturn(releases);
        mockedFiles.when(() -> Files.list(ARCHIVE_DIR)).thenReturn(Stream.of(zip1, zip2, zip3));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.walkFileTree(any(), any()), never());
        mockedFiles.verify(() -> Files.delete(any()), never());
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_withEmptyDirectory_shouldHandleGracefully() throws IOException {
        Release release1 = createRelease("8.0.0", "v8.0.0");
        List<Release> releases = List.of(release1);

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        when(releaseRepository.findAll()).thenReturn(releases);
        mockedFiles.when(() -> Files.list(ARCHIVE_DIR)).thenReturn(Stream.empty());

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.walkFileTree(any(), any()), never());
    }

    @Test
    public void downloadReleaseZipToPvc_withSpecialCharactersInTagName_shouldHandleCorrectly() throws IOException {
        String tagName = "v8.0.0-RC1";
        Path zipPath = ARCHIVE_DIR.resolve(tagName + ".zip");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        mockedFiles.when(() -> Files.exists(zipPath)).thenReturn(true);

        Path result = releaseArtifactService.downloadReleaseZipToPvc(tagName);

        assertEquals(zipPath, result);
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_withEmptyReleaseList_shouldDeleteAllZips() throws IOException {
        List<Release> releases = new ArrayList<>();

        Path zip1 = ARCHIVE_DIR.resolve("v1.0.0.zip");
        Path zip2 = ARCHIVE_DIR.resolve("v2.0.0.zip");
        Path zip3 = ARCHIVE_DIR.resolve("v3.0.0.zip");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        when(releaseRepository.findAll()).thenReturn(releases);
        mockedFiles.when(() -> Files.list(ARCHIVE_DIR)).thenReturn(Stream.of(zip1, zip2, zip3));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.list(ARCHIVE_DIR));
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_withSingleActiveRelease_shouldKeepOnlyActive() throws IOException {
        Release release = createRelease("9.0.0", "v9.0.0");
        List<Release> releases = List.of(release);

        Path activeZip = ARCHIVE_DIR.resolve("v9.0.0.zip");
        Path obsoleteZip1 = ARCHIVE_DIR.resolve("v8.0.0.zip");
        Path obsoleteZip2 = ARCHIVE_DIR.resolve("v7.0.0.zip");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        when(releaseRepository.findAll()).thenReturn(releases);
        mockedFiles.when(() -> Files.list(ARCHIVE_DIR)).thenReturn(Stream.of(activeZip, obsoleteZip1, obsoleteZip2));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.list(ARCHIVE_DIR));
    }

    @Test
    public void downloadReleaseZipToPvc_withVersionTag_shouldReturnCorrectPath() throws IOException {
        String tagName = "v10.0.0";
        Path zipPath = ARCHIVE_DIR.resolve(tagName + ".zip");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        mockedFiles.when(() -> Files.exists(zipPath)).thenReturn(true);

        Path result = releaseArtifactService.downloadReleaseZipToPvc(tagName);

        assertEquals(zipPath, result);
        assertEquals("v10.0.0.zip", result.getFileName().toString());
    }

    @Test
    public void downloadReleaseZipToPvc_withSnapshotTag_shouldReturnCorrectPath() throws IOException {
        String tagName = "v8.0.0-SNAPSHOT";
        Path zipPath = ARCHIVE_DIR.resolve(tagName + ".zip");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        mockedFiles.when(() -> Files.exists(zipPath)).thenReturn(true);

        Path result = releaseArtifactService.downloadReleaseZipToPvc(tagName);

        assertEquals(zipPath, result);
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_withOnlyActiveZips_shouldNotDeleteAny() throws IOException {
        Release release1 = createRelease("7.0.0", "v7.0.0");
        Release release2 = createRelease("7.5.0", "v7.5.0");
        Release release3 = createRelease("8.0.0", "v8.0.0");
        Release release4 = createRelease("8.5.0", "v8.5.0");
        List<Release> releases = List.of(release1, release2, release3, release4);

        Path zip1 = ARCHIVE_DIR.resolve("v7.0.0.zip");
        Path zip2 = ARCHIVE_DIR.resolve("v7.5.0.zip");
        Path zip3 = ARCHIVE_DIR.resolve("v8.0.0.zip");
        Path zip4 = ARCHIVE_DIR.resolve("v8.5.0.zip");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        when(releaseRepository.findAll()).thenReturn(releases);
        mockedFiles.when(() -> Files.list(ARCHIVE_DIR)).thenReturn(Stream.of(zip1, zip2, zip3, zip4));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.walkFileTree(any(), any()), never());
        mockedFiles.verify(() -> Files.delete(any()), never());
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_withHiddenFiles_shouldIgnoreThem() throws IOException {
        Release release = createRelease("8.0.0", "v8.0.0");
        List<Release> releases = List.of(release);

        Path activeZip = ARCHIVE_DIR.resolve("v8.0.0.zip");
        Path hiddenFile = ARCHIVE_DIR.resolve(".DS_Store");
        Path anotherHidden = ARCHIVE_DIR.resolve(".gitkeep");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        when(releaseRepository.findAll()).thenReturn(releases);
        mockedFiles.when(() -> Files.list(ARCHIVE_DIR)).thenReturn(Stream.of(activeZip, hiddenFile, anotherHidden));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.walkFileTree(any(), any()), never());
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_withSubdirectories_shouldIgnoreThem() throws IOException {
        Release release = createRelease("8.0.0", "v8.0.0");
        List<Release> releases = List.of(release);

        Path activeZip = ARCHIVE_DIR.resolve("v8.0.0.zip");
        Path subDir1 = ARCHIVE_DIR.resolve("extracted-v7.0.0");
        Path subDir2 = ARCHIVE_DIR.resolve("backup");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        when(releaseRepository.findAll()).thenReturn(releases);
        mockedFiles.when(() -> Files.list(ARCHIVE_DIR)).thenReturn(Stream.of(activeZip, subDir1, subDir2));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.walkFileTree(any(), any()), never());
    }

    @Test
    public void downloadReleaseZipToPvc_withBetaTag_shouldReturnCorrectPath() throws IOException {
        String tagName = "v8.0.0-beta.1";
        Path zipPath = ARCHIVE_DIR.resolve(tagName + ".zip");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        mockedFiles.when(() -> Files.exists(zipPath)).thenReturn(true);

        Path result = releaseArtifactService.downloadReleaseZipToPvc(tagName);

        assertEquals(zipPath, result);
    }

    @Test
    public void downloadReleaseZipToPvc_withMilestoneTag_shouldReturnCorrectPath() throws IOException {
        String tagName = "v8.0.0-M1";
        Path zipPath = ARCHIVE_DIR.resolve(tagName + ".zip");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        mockedFiles.when(() -> Files.exists(zipPath)).thenReturn(true);

        Path result = releaseArtifactService.downloadReleaseZipToPvc(tagName);

        assertEquals(zipPath, result);
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_withZipAndTarGz_shouldOnlyProcessZipFiles() throws IOException {
        Release release = createRelease("8.0.0", "v8.0.0");
        List<Release> releases = List.of(release);

        Path activeZip = ARCHIVE_DIR.resolve("v8.0.0.zip");
        Path tarGz = ARCHIVE_DIR.resolve("v7.0.0.tar.gz");
        Path tarFile = ARCHIVE_DIR.resolve("v6.0.0.tar");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        when(releaseRepository.findAll()).thenReturn(releases);
        mockedFiles.when(() -> Files.list(ARCHIVE_DIR)).thenReturn(Stream.of(activeZip, tarGz, tarFile));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.walkFileTree(any(), any()), never());
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_withCaseVariations_shouldMatchCorrectly() throws IOException {
        Release release = createRelease("8.0.0", "v8.0.0");
        List<Release> releases = List.of(release);

        Path activeZip = ARCHIVE_DIR.resolve("v8.0.0.zip");
        Path uppercaseZip = ARCHIVE_DIR.resolve("V7.0.0.ZIP");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        when(releaseRepository.findAll()).thenReturn(releases);
        mockedFiles.when(() -> Files.list(ARCHIVE_DIR)).thenReturn(Stream.of(activeZip, uppercaseZip));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.list(ARCHIVE_DIR));
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_withManyObsoleteZips_shouldAttemptToDeleteAll() throws IOException {
        Release release = createRelease("10.0.0", "v10.0.0");
        List<Release> releases = List.of(release);

        Path activeZip = ARCHIVE_DIR.resolve("v10.0.0.zip");
        Path obsolete1 = ARCHIVE_DIR.resolve("v1.0.0.zip");
        Path obsolete2 = ARCHIVE_DIR.resolve("v2.0.0.zip");
        Path obsolete3 = ARCHIVE_DIR.resolve("v3.0.0.zip");
        Path obsolete4 = ARCHIVE_DIR.resolve("v4.0.0.zip");
        Path obsolete5 = ARCHIVE_DIR.resolve("v5.0.0.zip");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        when(releaseRepository.findAll()).thenReturn(releases);
        mockedFiles
                .when(() -> Files.list(ARCHIVE_DIR))
                .thenReturn(Stream.of(activeZip, obsolete1, obsolete2, obsolete3, obsolete4, obsolete5));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.list(ARCHIVE_DIR));
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_withNullTagName_shouldHandleGracefully() throws IOException {
        Release releaseWithNullTag = new Release();
        releaseWithNullTag.setName("Test Release");
        releaseWithNullTag.setTagName(null);

        Release validRelease = createRelease("8.0.0", "v8.0.0");
        List<Release> releases = List.of(validRelease);

        Path activeZip = ARCHIVE_DIR.resolve("v8.0.0.zip");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        when(releaseRepository.findAll()).thenReturn(releases);
        mockedFiles.when(() -> Files.list(ARCHIVE_DIR)).thenReturn(Stream.of(activeZip));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.walkFileTree(any(), any()), never());
    }

    @Test
    public void downloadReleaseZipToPvc_withNumericTag_shouldReturnCorrectPath() throws IOException {
        String tagName = "7.8";
        Path zipPath = ARCHIVE_DIR.resolve(tagName + ".zip");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        mockedFiles.when(() -> Files.exists(zipPath)).thenReturn(true);

        Path result = releaseArtifactService.downloadReleaseZipToPvc(tagName);

        assertEquals(zipPath, result);
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_withLogFiles_shouldIgnoreThem() throws IOException {
        Release release = createRelease("8.0.0", "v8.0.0");
        List<Release> releases = List.of(release);

        Path activeZip = ARCHIVE_DIR.resolve("v8.0.0.zip");
        Path logFile = ARCHIVE_DIR.resolve("download.log");
        Path errorLog = ARCHIVE_DIR.resolve("error.log");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        when(releaseRepository.findAll()).thenReturn(releases);
        mockedFiles.when(() -> Files.list(ARCHIVE_DIR)).thenReturn(Stream.of(activeZip, logFile, errorLog));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.walkFileTree(any(), any()), never());
    }

    @Test
    public void deleteObsoleteReleaseArtifacts_withPartialZipNames_shouldNotMatchWrongly() throws IOException {
        Release release = createRelease("8.0.0", "v8.0.0");
        List<Release> releases = List.of(release);

        Path activeZip = ARCHIVE_DIR.resolve("v8.0.0.zip");
        Path partialMatch = ARCHIVE_DIR.resolve("v8.0.0-extra.zip");
        Path prefixMatch = ARCHIVE_DIR.resolve("prefix-v8.0.0.zip");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        when(releaseRepository.findAll()).thenReturn(releases);
        mockedFiles.when(() -> Files.list(ARCHIVE_DIR)).thenReturn(Stream.of(activeZip, partialMatch, prefixMatch));

        releaseArtifactService.deleteObsoleteReleaseArtifacts();

        mockedFiles.verify(() -> Files.list(ARCHIVE_DIR));
    }

    @Test
    public void downloadReleaseZipToPvc_returnsExpectedZipPathFormat() throws IOException {
        String tagName = "v7.9.1";
        Path expectedZipPath = ARCHIVE_DIR.resolve("v7.9.1.zip");

        mockedFiles.when(() -> Files.exists(ARCHIVE_DIR)).thenReturn(true);
        mockedFiles.when(() -> Files.exists(expectedZipPath)).thenReturn(true);

        Path result = releaseArtifactService.downloadReleaseZipToPvc(tagName);

        Assertions.assertTrue(result.toString().endsWith(".zip"));
        Assertions.assertTrue(result.toString().contains(tagName));
    }
}
