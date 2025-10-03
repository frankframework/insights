package org.frankframework.insights.release;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ReleaseArtifactService {

	private static final Path ARCHIVE_DIR = Paths.get("release-archive");
	private static final String GITHUB_ZIP_URL_FORMAT =
			"https://github.com/frankframework/frankframework/archive/refs/tags/%s.zip";

	private static final int BUFFER_SIZE = 4096;
	private static final long MAX_ARCHIVE_SIZE = 1024L * 1024 * 1024;
	private static final int MAX_ENTRIES = 1024;
	private static final double COMPRESSION_RATIO_LIMIT = 10.0;

	@Transactional
	public Path prepareReleaseArtifacts(Release release) throws IOException {
		Path releaseDir = ARCHIVE_DIR.resolve(release.getName());

		if (releaseDirectoryExists(releaseDir, release)) {
			return releaseDir;
		}

		String zipUrl = buildZipUrl(release);
		Path zipFile = downloadReleaseZip(releaseDir, zipUrl, release);
		unpackAndCleanup(zipFile, releaseDir);

		return releaseDir;
	}

	private boolean releaseDirectoryExists(Path releaseDir, Release release) throws IOException {
		if (Files.isDirectory(releaseDir)) {
			try (Stream<Path> stream = Files.list(releaseDir)) {
				if (stream.findFirst().isPresent()) {
					log.info("Source code for release {} already exists, skipping download.", release.getName());
					return true;
				}
			}
		}
		return false;
	}

	private String buildZipUrl(Release release) throws IOException {
		String tagName = release.getTagName();
		if (tagName == null || tagName.isBlank()) {
			throw new IOException("Release " + release.getName() + " is missing a tagName.");
		}
		return String.format(GITHUB_ZIP_URL_FORMAT, tagName);
	}

	private Path downloadReleaseZip(Path releaseDir, String zipUrl, Release release) throws IOException {
		log.info("Downloading source for {} from {}", release.getName(), zipUrl);
		Files.createDirectories(releaseDir);
		Path zipFile = releaseDir.resolve(release.getName() + ".zip");

		try (InputStream in = URI.create(zipUrl).toURL().openStream()) {
			Files.copy(in, zipFile, StandardCopyOption.REPLACE_EXISTING);
		}
		return zipFile;
	}

	private void unpackAndCleanup(Path zipFile, Path releaseDir) throws IOException {
		log.debug("Unpacking archive for {}", zipFile.getFileName());
		unzip(zipFile, releaseDir);
		Files.delete(zipFile);
		log.debug("Successfully downloaded and unpacked source for {}", zipFile.getFileName());
	}

	/**
	 * Securely unzips a file using the java.nio.file.FileSystem API for robust traversal.
	 * This approach is clearer for static analysis tools and avoids manual entry iteration.
	 */
	private void unzip(Path zipFile, Path destDir) throws IOException {
		Path normalizedDestDir = destDir.toAbsolutePath().normalize();
		AtomicInteger entryCount = new AtomicInteger(0);
		AtomicLong totalUncompressedSize = new AtomicLong(0);

		try (FileSystem zipFs = FileSystems.newFileSystem(zipFile, (ClassLoader) null);
			 ZipFile zf = new ZipFile(zipFile.toFile())) {

			Path root = zipFs.getPath("/");
			try (Stream<Path> stream = Files.walk(root)) {
				stream.forEach(path -> {
					try {
						if (entryCount.incrementAndGet() > MAX_ENTRIES) {
							throw new IOException("Archive contains too many entries.");
						}
						processPath(path, normalizedDestDir, totalUncompressedSize, zf);
					} catch (IOException e) {
						throw new RuntimeException(e); // Will be caught by the outer catch block
					}
				});
			}
		} catch (RuntimeException e) {
			// Un-wrap the IOException from the lambda
			if (e.getCause() instanceof IOException) {
				throw (IOException) e.getCause();
			}
			throw e;
		}
	}

	/**
	 * Processes a single path from the zip file system, handling directories and files.
	 */
	private void processPath(Path pathInZip, Path destDir, AtomicLong totalUncompressedSize, ZipFile zf) throws IOException {
		Path validatedPath = validateAndStripPath(pathInZip, destDir);
		if (validatedPath == null) {
			return; // Skip top-level directory
		}

		if (Files.isDirectory(pathInZip)) {
			Files.createDirectories(validatedPath);
		} else {
			extractAndValidateFile(pathInZip, validatedPath, totalUncompressedSize, zf);
		}
	}

	/**
	 * Validates and prepares the destination path, preventing path traversal.
	 */
	private Path validateAndStripPath(Path pathInZip, Path destDir) throws IOException {
		if (pathInZip.getNameCount() <= 1) {
			return null; // Ignore the root and GitHub's top-level directory
		}
		Path strippedPath = pathInZip.subpath(1, pathInZip.getNameCount());
		Path resolvedPath = destDir.resolve(strippedPath.toString()).normalize();
		if (!resolvedPath.startsWith(destDir)) {
			throw new IOException("Bad zip entry: " + pathInZip + " (Path Traversal attempt)");
		}
		return resolvedPath;
	}

	/**
	 * Extracts a file while validating its size and compression ratio.
	 */
	private void extractAndValidateFile(Path pathInZip, Path destFile, AtomicLong totalUncompressedSize, ZipFile zf) throws IOException {
		Files.createDirectories(destFile.getParent());
		ZipEntry entry = zf.getEntry(pathInZip.toString().substring(1)); // ZipFile needs entry name without leading '/'
		if (entry == null) {
			throw new IOException("Could not find ZipEntry for path: " + pathInZip);
		}

		long totalEntrySize = 0;
		byte[] buffer = new byte[BUFFER_SIZE];
		try (InputStream in = zf.getInputStream(entry);
			 var out = Files.newOutputStream(destFile)) {
			int nBytes;
			while ((nBytes = in.read(buffer)) > 0) {
				out.write(buffer, 0, nBytes);
				totalEntrySize += nBytes;

				if (totalUncompressedSize.addAndGet(nBytes) > MAX_ARCHIVE_SIZE) {
					throw new IOException("Archive is too large when uncompressed.");
				}

				if (entry.getCompressedSize() > 0) {
					double ratio = (double) totalEntrySize / entry.getCompressedSize();
					if (ratio > COMPRESSION_RATIO_LIMIT) {
						throw new IOException("Compression ratio for entry " + entry.getName() + " is too high.");
					}
				}
			}
		}
	}
}

