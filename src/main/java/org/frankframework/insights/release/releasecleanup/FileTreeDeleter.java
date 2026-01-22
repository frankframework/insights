package org.frankframework.insights.release.releasecleanup;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileTreeDeleter {

    private static final String RM_EXECUTABLE = "/bin/rm";

    public void deleteTreeRecursively(Path path) throws IOException, InterruptedException {
        if (path == null) {
            return;
        }

        Path normalizedPath = path.toAbsolutePath().normalize();

        if (!Files.exists(normalizedPath, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }

        try {
            Files.walkFileTree(
                    normalizedPath, Collections.emptySet(), Integer.MAX_VALUE, new DeleteTreeVisitor(null, null));
        } catch (IOException e) {
            log.warn("Default deletion failed for {}: {}", normalizedPath, e.getMessage());
            fallbackDelete(normalizedPath);
        }
    }

    private void fallbackDelete(Path path) throws IOException, InterruptedException {
        if (!Files.exists(Path.of(RM_EXECUTABLE))) {
            throw new IOException("Cannot delete directory: rm not available at " + RM_EXECUTABLE);
        }

        ProcessBuilder pb = new ProcessBuilder(RM_EXECUTABLE, "-rf", path.toString());
        pb.environment().clear();
        pb.environment().put("PATH", "/bin:/usr/bin");

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("Native rm failed with exit code: " + exitCode);
        }
    }
}
