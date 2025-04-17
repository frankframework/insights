package org.frankframework.insights.release;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/releases")
public class ReleaseController {
    private final ReleaseService releaseService;

    public ReleaseController(ReleaseService releaseService) {
        this.releaseService = releaseService;
    }

    @GetMapping
    public ResponseEntity<Set<ReleaseResponse>> getAllReleases() {
        Set<ReleaseResponse> releases = releaseService.getAllReleases();
        log.info("Successfully retrieved {} release responses", releases.size());
        return ResponseEntity.status(HttpStatus.OK).body(releases);
    }

    @GetMapping("/{releaseId}")
    public ResponseEntity<ReleaseResponse> getReleaseById(@PathVariable String releaseId)
            throws ReleaseNotFoundException {
        ReleaseResponse release = releaseService.getReleaseById(releaseId);
        log.info("Successfully retrieved release [{}]", release.id());
        return ResponseEntity.status(HttpStatus.OK).body(release);
    }
}
