package org.frankframework.insights.release.releasecleanup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileTreeDeleter {
    public void deleteTreeRecursively(Path path) throws IOException, InterruptedException {
        if (!Files.exists(path)) return;

        try {
            Files.walkFileTree(path, new DeleteTreeVisitor(null, null));
        } catch (IOException e) {
            log.warn("default deletion failed because of {}, convert to native rm", path);
            new ProcessBuilder("rm", "-rf", path.toString()).start().waitFor();
        }
    }
}
