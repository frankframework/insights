package org.frankframework.insights.issue;

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
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/issues")
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
     * Fetches all epic issues that are planned for the future (i.e., with a due date after the current date).
     * @return Set of future epic issues
     */
    @GetMapping("/future")
    public ResponseEntity<Set<IssueResponse>> getFutureEpicIssues() {
        Set<IssueResponse> futureIssues = issueService.getFutureEpicIssues();
        if (futureIssues == null) futureIssues = Collections.emptySet();
        return ResponseEntity.status(HttpStatus.OK).body(futureIssues);
    }
}
