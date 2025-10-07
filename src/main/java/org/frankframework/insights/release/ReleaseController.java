package org.frankframework.insights.release;

import java.util.Collections;
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
@RequestMapping("/releases")
public class ReleaseController {
    private final ReleaseService releaseService;

    public ReleaseController(ReleaseService releaseService) {
        this.releaseService = releaseService;
    }

    /**
     * Fetches all releases from the database.
     * @return Set of ReleaseResponse objects representing all releases
     */
    @GetMapping
    public ResponseEntity<Set<ReleaseResponse>> getAllReleases() {
        Set<ReleaseResponse> releases = releaseService.getAllReleases();
        if (releases == null) releases = Collections.emptySet();
        return ResponseEntity.status(HttpStatus.OK).body(releases);
    }

    /**
     * Fetches a single release by ID from the database.
     * @param releaseId The ID of the release to fetch
     * @return ReleaseResponse object representing the release
     * @throws ReleaseNotFoundException if the release is not found
     */
    @GetMapping("/{releaseId}")
    public ResponseEntity<ReleaseResponse> getReleaseById(@PathVariable String releaseId)
            throws ReleaseNotFoundException {
        ReleaseResponse release = releaseService.getReleaseById(releaseId);
        return ResponseEntity.status(HttpStatus.OK).body(release);
    }
}
