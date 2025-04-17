package org.frankframework.insights.issue;

import java.time.OffsetDateTime;
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

    @GetMapping("/release/{releaseId}")
    public ResponseEntity<Set<IssueResponse>> getIssuesByReleaseId(@PathVariable String releaseId)
            throws ReleaseNotFoundException {
        Set<IssueResponse> releaseIssues = issueService.getIssuesByReleaseId(releaseId);
        log.info("Successfully fetched {} issues for release with ID [{}]", releaseIssues.size(), releaseId);
        return ResponseEntity.status(HttpStatus.OK).body(releaseIssues);
    }

    @GetMapping("/milestone/{milestoneId}")
    public ResponseEntity<Set<IssueResponse>> getIssuesByMilestoneId(@PathVariable String milestoneId)
            throws MilestoneNotFoundException {
        Set<IssueResponse> milestoneIssues = issueService.getIssuesByMilestoneId(milestoneId);
        log.info("Successfully fetched {} issues for milestone with ID [{}]", milestoneIssues.size(), milestoneId);
        return ResponseEntity.status(HttpStatus.OK).body(milestoneIssues);
    }

    @GetMapping("/timespan")
    public ResponseEntity<Set<IssueResponse>> getIssuesByTimespan(
            @RequestParam OffsetDateTime startDate, @RequestParam OffsetDateTime endDate) {
        Set<IssueResponse> milestoneIssues = issueService.getIssuesByTimespan(startDate, endDate);
        log.info("Successfully fetched {} issues between {} and {}", milestoneIssues.size(), startDate, endDate);
        return ResponseEntity.status(HttpStatus.OK).body(milestoneIssues);
    }
}
