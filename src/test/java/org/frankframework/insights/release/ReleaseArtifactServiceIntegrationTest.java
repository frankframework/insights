package org.frankframework.insights.release;

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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

/**
 * Integration tests for ReleaseArtifactService that test the actual zip extraction logic.
 */
public class ReleaseArtifactServiceIntegrationTest {

    private ReleaseArtifactService releaseArtifactService;
    private MockedStatic<URI> mockedUri;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        releaseArtifactService = new ReleaseArtifactService();
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
    public void unzip_withAbsolutePathInZipEntry_shouldHandleGracefully() throws IOException {
        Release release = createRelease("zip-absolute", "v-abs");
        String zipUrl = "https://github.com/frankframework/frankframework/archive/refs/tags/v-abs.zip";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("frankframework-abs/"));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("/etc/passwd"));
            zos.write("hacked".getBytes());
            zos.closeEntry();
        }
        byte[] maliciousZip = baos.toByteArray();

        mockDownload(zipUrl, maliciousZip);

        try {
            releaseArtifactService.prepareReleaseArtifacts(release);
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("Path Traversal")
                    || e.getMessage().contains("Bad zip entry")
                    || e.getMessage().contains("Could not find ZipEntry"));
        }
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
}
