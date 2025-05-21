package org.frankframework.insights.issue;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.milestone.MilestoneNotFoundException;
import org.frankframework.insights.release.ReleaseNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/issues")
public class IssueController {
    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    /**
     * Fetches all issues associated with a given release ID.
     * @param releaseId The ID of the release to fetch issues for
     * @return Set of issues associated with the release
     * @throws ReleaseNotFoundException if the release is not found
     */
    @GetMapping("/release/{releaseId}")
    public ResponseEntity<Set<IssueResponse>> getIssuesByReleaseId(@PathVariable String releaseId)
            throws ReleaseNotFoundException {
        Set<IssueResponse> releaseIssues = issueService.getIssuesByReleaseId(releaseId);
        if (releaseIssues == null) releaseIssues = Collections.emptySet();
        return ResponseEntity.status(HttpStatus.OK).body(releaseIssues);
    }

    /**
     * Fetches all issues associated with a given milestone ID.
     * @param milestoneId The ID of the milestone to fetch issues for
     * @return Set of issues associated with the milestone
     * @throws MilestoneNotFoundException if the milestone is not found
     */
    @GetMapping("/milestone/{milestoneId}")
    public ResponseEntity<Set<IssueResponse>> getIssuesByMilestoneId(@PathVariable String milestoneId)
            throws MilestoneNotFoundException {
        Set<IssueResponse> milestoneIssues = issueService.getIssuesByMilestoneId(milestoneId);
        if (milestoneIssues == null) milestoneIssues = Collections.emptySet();
        return ResponseEntity.status(HttpStatus.OK).body(milestoneIssues);
    }

    /**
     * Fetches all issues associated with a given timespan.
     * @param startDate the start date of the timespan
     * @param endDate  the end date of the timespan
     * @return Set of issues made between the given timestamps the timespan
     */
    @GetMapping("/timespan")
    public ResponseEntity<Set<IssueResponse>> getIssuesByTimespan(
            @RequestParam OffsetDateTime startDate, @RequestParam OffsetDateTime endDate) {
        Set<IssueResponse> milestoneIssues = issueService.getIssuesByTimespan(startDate, endDate);
        if (milestoneIssues == null) milestoneIssues = Collections.emptySet();
        return ResponseEntity.status(HttpStatus.OK).body(milestoneIssues);
    }
}
