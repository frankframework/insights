package org.frankframework.insights.label;

import java.util.Collections;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.mapper.MappingException;
import org.frankframework.insights.release.ReleaseNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/labels")
public class LabelController {
    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    /**
     * Fetches highlights by release ID.
     * @param releaseId The ID of the release to fetch highlights for
     * @return ResponseEntity containing a set of LabelResponse objects
     * @throws ReleaseNotFoundException if the release is not found
     * @throws MappingException if an error occurs during the mapping process
     */
    @GetMapping("/release/{releaseId}")
    public ResponseEntity<HighlightsResponse> getHighlightsByReleaseId(@PathVariable String releaseId)
            throws ReleaseNotFoundException, MappingException {
        HighlightsResponse releaseHighlights = labelService.getHighlightsByReleaseId(releaseId);
        return ResponseEntity.status(HttpStatus.OK).body(releaseHighlights);
    }
}
