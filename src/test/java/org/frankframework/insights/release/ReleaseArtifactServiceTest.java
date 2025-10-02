package org.frankframework.insights.release;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
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
	private static final Path ARCHIVE_DIR = Paths.get("release-archive");

	@BeforeEach
	public void setUp() {
		releaseArtifactService = new ReleaseArtifactService();
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
	public void prepareReleaseArtifacts_happyFlow_shouldDownloadAndUnpack() throws IOException {
		Release release = createRelease("7.8.0", "v7.8.0");
		Path releaseDir = ARCHIVE_DIR.resolve(release.getName());
		Path zipFile = releaseDir.resolve(release.getName() + ".zip");
		String zipUrl = "https://github.com/frankframework/frankframework/archive/refs/tags/v7.8.0.zip";

		mockedFiles.when(() -> Files.isDirectory(releaseDir)).thenReturn(false);

		URL urlMock = mock(URL.class);
		URI uriMock = mock(URI.class);
		when(uriMock.toURL()).thenReturn(urlMock);
		mockedUri.when(() -> URI.create(zipUrl)).thenReturn(uriMock);
		byte[] validZipBytes;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
			 ZipOutputStream zos = new ZipOutputStream(baos)) {
			zos.putNextEntry(new ZipEntry("frankframework-7.8.0/"));
			zos.closeEntry();
			zos.putNextEntry(new ZipEntry("frankframework-7.8.0/file.txt"));
			zos.write("content".getBytes());
			zos.closeEntry();
			zos.finish();
			validZipBytes = baos.toByteArray();
		}
		when(urlMock.openStream()).thenReturn(new ByteArrayInputStream(validZipBytes));

		mockedFiles.when(() -> Files.createDirectories(releaseDir)).thenReturn(releaseDir);
		mockedFiles.when(() -> Files.copy(any(InputStream.class), eq(zipFile), any(StandardCopyOption.class))).thenReturn((long) validZipBytes.length);
		mockedFiles.when(() -> Files.newInputStream(zipFile)).thenReturn(new ByteArrayInputStream(validZipBytes));
		mockedFiles.when(() -> Files.delete(zipFile)).thenAnswer(invocation -> null);

		Path result = releaseArtifactService.prepareReleaseArtifacts(release);

		assertEquals(releaseDir, result);
		verify(urlMock).openStream();
		mockedFiles.verify(() -> Files.copy(any(InputStream.class), eq(zipFile), any(StandardCopyOption.class)));
		mockedFiles.verify(() -> Files.delete(zipFile));
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
		mockedFiles.verify(() -> Files.copy(any(InputStream.class), any(Path.class), any(StandardCopyOption.class)), never());
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
	public void prepareReleaseArtifacts_withEmptyZip_shouldSucceedWithEmptyDir() throws IOException {
		Release release = createRelease("empty", "v-empty");
		Path releaseDir = ARCHIVE_DIR.resolve(release.getName());
		Path zipFile = releaseDir.resolve(release.getName() + ".zip");
		String zipUrl = "https://github.com/frankframework/frankframework/archive/refs/tags/v-empty.zip";

		mockedFiles.when(() -> Files.isDirectory(releaseDir)).thenReturn(false);

		URL urlMock = mock(URL.class);
		URI uriMock = mock(URI.class);
		when(uriMock.toURL()).thenReturn(urlMock);
		mockedUri.when(() -> URI.create(zipUrl)).thenReturn(uriMock);
		byte[] emptyZipBytes;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ZipOutputStream zos = new ZipOutputStream(baos)) {
			zos.finish();
			emptyZipBytes = baos.toByteArray();
		}
		when(urlMock.openStream()).thenReturn(new ByteArrayInputStream(emptyZipBytes));

		mockedFiles.when(() -> Files.copy(any(InputStream.class), eq(zipFile), any(StandardCopyOption.class))).thenReturn((long) emptyZipBytes.length);
		mockedFiles.when(() -> Files.newInputStream(zipFile)).thenReturn(new ByteArrayInputStream(emptyZipBytes));
		mockedFiles.when(() -> Files.delete(zipFile)).thenAnswer(inv -> null);

		releaseArtifactService.prepareReleaseArtifacts(release);

		mockedFiles.verify(() -> Files.delete(zipFile));
		mockedFiles.verify(() -> Files.copy(any(ZipInputStream.class), any(Path.class), any(StandardCopyOption.class)), never());
	}

	@Test
	public void prepareReleaseArtifacts_whenDirectoryExistsButIsEmpty_shouldDownload() throws IOException {
		Release release = createRelease("7.9.0", "v7.9.0");
		Path releaseDir = ARCHIVE_DIR.resolve(release.getName());
		Path zipFile = releaseDir.resolve(release.getName() + ".zip");
		String zipUrl = "https://github.com/frankframework/frankframework/archive/refs/tags/v7.9.0.zip";

		mockedFiles.when(() -> Files.isDirectory(releaseDir)).thenReturn(true);
		mockedFiles.when(() -> Files.list(releaseDir)).thenReturn(Stream.empty());

		URL urlMock = mock(URL.class);
		URI uriMock = mock(URI.class);
		when(uriMock.toURL()).thenReturn(urlMock);
		mockedUri.when(() -> URI.create(zipUrl)).thenReturn(uriMock);
		byte[] validZipBytes;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
			 ZipOutputStream zos = new ZipOutputStream(baos)) {
			zos.putNextEntry(new ZipEntry("frankframework-7.9.0/"));
			zos.closeEntry();
			zos.putNextEntry(new ZipEntry("frankframework-7.9.0/file.txt"));
			zos.write("content".getBytes());
			zos.closeEntry();
			zos.finish();
			validZipBytes = baos.toByteArray();
		}
		when(urlMock.openStream()).thenReturn(new ByteArrayInputStream(validZipBytes));

		mockedFiles.when(() -> Files.createDirectories(releaseDir)).thenReturn(releaseDir);
		mockedFiles.when(() -> Files.copy(any(InputStream.class), eq(zipFile), any(StandardCopyOption.class))).thenReturn((long) validZipBytes.length);
		mockedFiles.when(() -> Files.newInputStream(zipFile)).thenReturn(new ByteArrayInputStream(validZipBytes));
		mockedFiles.when(() -> Files.delete(zipFile)).thenAnswer(invocation -> null);

		Path result = releaseArtifactService.prepareReleaseArtifacts(release);

		assertEquals(releaseDir, result);
		verify(urlMock).openStream();
	}

	@Test
	public void prepareReleaseArtifacts_withBlankTagName_shouldThrowIOException() {
		Release release = createRelease("blank-tag", "   ");
		Exception e = assertThrows(IOException.class, () -> releaseArtifactService.prepareReleaseArtifacts(release));
		assertTrue(e.getMessage().contains("is missing a tagName"));
	}

	@Test
	public void prepareReleaseArtifacts_withNestedDirectoriesInZip_shouldExtractCorrectly() throws IOException {
		Release release = createRelease("nested", "v-nested");
		Path releaseDir = ARCHIVE_DIR.resolve(release.getName());
		Path zipFile = releaseDir.resolve(release.getName() + ".zip");
		String zipUrl = "https://github.com/frankframework/frankframework/archive/refs/tags/v-nested.zip";

		mockedFiles.when(() -> Files.isDirectory(releaseDir)).thenReturn(false);

		URL urlMock = mock(URL.class);
		URI uriMock = mock(URI.class);
		when(uriMock.toURL()).thenReturn(urlMock);
		mockedUri.when(() -> URI.create(zipUrl)).thenReturn(uriMock);

		byte[] nestedZipBytes;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
			 ZipOutputStream zos = new ZipOutputStream(baos)) {
			zos.putNextEntry(new ZipEntry("frankframework-nested/"));
			zos.closeEntry();
			zos.putNextEntry(new ZipEntry("frankframework-nested/src/"));
			zos.closeEntry();
			zos.putNextEntry(new ZipEntry("frankframework-nested/src/main/"));
			zos.closeEntry();
			zos.putNextEntry(new ZipEntry("frankframework-nested/src/main/java/"));
			zos.closeEntry();
			zos.putNextEntry(new ZipEntry("frankframework-nested/src/main/java/Test.java"));
			zos.write("public class Test {}".getBytes());
			zos.closeEntry();
			zos.finish();
			nestedZipBytes = baos.toByteArray();
		}
		when(urlMock.openStream()).thenReturn(new ByteArrayInputStream(nestedZipBytes));

		mockedFiles.when(() -> Files.createDirectories(releaseDir)).thenReturn(releaseDir);
		mockedFiles.when(() -> Files.copy(any(InputStream.class), eq(zipFile), any(StandardCopyOption.class))).thenReturn((long) nestedZipBytes.length);
		mockedFiles.when(() -> Files.newInputStream(zipFile)).thenReturn(new ByteArrayInputStream(nestedZipBytes));
		mockedFiles.when(() -> Files.delete(zipFile)).thenAnswer(inv -> null);

		Path result = releaseArtifactService.prepareReleaseArtifacts(release);

		assertEquals(releaseDir, result);
		mockedFiles.verify(() -> Files.delete(zipFile));
	}

	@Test
	public void prepareReleaseArtifacts_withTopLevelFileOnlyInZip_shouldSkipIt() throws IOException {
		Release release = createRelease("top-level", "v-top-level");
		Path releaseDir = ARCHIVE_DIR.resolve(release.getName());
		Path zipFile = releaseDir.resolve(release.getName() + ".zip");
		String zipUrl = "https://github.com/frankframework/frankframework/archive/refs/tags/v-top-level.zip";

		mockedFiles.when(() -> Files.isDirectory(releaseDir)).thenReturn(false);

		URL urlMock = mock(URL.class);
		URI uriMock = mock(URI.class);
		when(uriMock.toURL()).thenReturn(urlMock);
		mockedUri.when(() -> URI.create(zipUrl)).thenReturn(uriMock);

		byte[] topLevelZipBytes;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
			 ZipOutputStream zos = new ZipOutputStream(baos)) {
			zos.putNextEntry(new ZipEntry("README.md"));
			zos.write("readme content".getBytes());
			zos.closeEntry();
			zos.finish();
			topLevelZipBytes = baos.toByteArray();
		}
		when(urlMock.openStream()).thenReturn(new ByteArrayInputStream(topLevelZipBytes));

		mockedFiles.when(() -> Files.createDirectories(releaseDir)).thenReturn(releaseDir);
		mockedFiles.when(() -> Files.copy(any(InputStream.class), eq(zipFile), any(StandardCopyOption.class))).thenReturn((long) topLevelZipBytes.length);
		mockedFiles.when(() -> Files.newInputStream(zipFile)).thenReturn(new ByteArrayInputStream(topLevelZipBytes));
		mockedFiles.when(() -> Files.delete(zipFile)).thenAnswer(inv -> null);

		releaseArtifactService.prepareReleaseArtifacts(release);

		mockedFiles.verify(() -> Files.copy(any(ZipInputStream.class), any(Path.class), any(StandardCopyOption.class)), never());
	}

	@Test
	public void prepareReleaseArtifacts_whenURICreationFails_shouldThrowException() throws IOException {
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
	public void prepareReleaseArtifacts_withSpecialCharactersInTagName_shouldBuildCorrectURL() throws IOException {
		Release release = createRelease("special", "v1.0.0-RC1");
		Path releaseDir = ARCHIVE_DIR.resolve(release.getName());
		Path zipFile = releaseDir.resolve(release.getName() + ".zip");
		String zipUrl = "https://github.com/frankframework/frankframework/archive/refs/tags/v1.0.0-RC1.zip";

		mockedFiles.when(() -> Files.isDirectory(releaseDir)).thenReturn(false);

		URL urlMock = mock(URL.class);
		URI uriMock = mock(URI.class);
		when(uriMock.toURL()).thenReturn(urlMock);
		mockedUri.when(() -> URI.create(zipUrl)).thenReturn(uriMock);

		byte[] validZipBytes;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
			 ZipOutputStream zos = new ZipOutputStream(baos)) {
			zos.putNextEntry(new ZipEntry("frankframework-1.0.0-RC1/"));
			zos.closeEntry();
			zos.finish();
			validZipBytes = baos.toByteArray();
		}
		when(urlMock.openStream()).thenReturn(new ByteArrayInputStream(validZipBytes));

		mockedFiles.when(() -> Files.createDirectories(releaseDir)).thenReturn(releaseDir);
		mockedFiles.when(() -> Files.copy(any(InputStream.class), eq(zipFile), any(StandardCopyOption.class))).thenReturn((long) validZipBytes.length);
		mockedFiles.when(() -> Files.newInputStream(zipFile)).thenReturn(new ByteArrayInputStream(validZipBytes));
		mockedFiles.when(() -> Files.delete(zipFile)).thenAnswer(inv -> null);

		Path result = releaseArtifactService.prepareReleaseArtifacts(release);

		assertEquals(releaseDir, result);
		mockedUri.verify(() -> URI.create(zipUrl));
	}
}
