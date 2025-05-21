package org.frankframework.insights.release;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.frankframework.insights.common.entityconnection.pullrequestissue.PullRequestIssue;
import org.frankframework.insights.common.entityconnection.pullrequestissue.PullRequestIssueRepository;
import org.frankframework.insights.common.entityconnection.releasepullrequest.ReleasePullRequest;
import org.frankframework.insights.common.entityconnection.releasepullrequest.ReleasePullRequestRepository;
import org.frankframework.insights.common.helper.ReleaseIssueHelperService;
import org.frankframework.insights.issue.Issue;
import org.frankframework.insights.pullrequest.PullRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReleaseIssueHelperServiceTest {

	private ReleaseService releaseService;
	private ReleasePullRequestRepository releasePullRequestRepository;
	private PullRequestIssueRepository pullRequestIssueRepository;

	private ReleaseIssueHelperService helperService;

	@BeforeEach
	void setup() {
		releaseService = mock(ReleaseService.class);
		releasePullRequestRepository = mock(ReleasePullRequestRepository.class);
		pullRequestIssueRepository = mock(PullRequestIssueRepository.class);

		helperService = new ReleaseIssueHelperService(releaseService, releasePullRequestRepository, pullRequestIssueRepository);
	}

	@Test
	public void getIssuesByReleaseId_happyPath_returnsIssues() throws Exception {
		String releaseId = "release-123";
		Release release = new Release();
		release.setId(releaseId);

		PullRequest pr1 = new PullRequest();
		pr1.setId("pr-1");
		PullRequest pr2 = new PullRequest();
		pr2.setId("pr-2");

		ReleasePullRequest relPr1 = new ReleasePullRequest(release, pr1);
		ReleasePullRequest relPr2 = new ReleasePullRequest(release, pr2);

		Issue issue1 = new Issue();
		issue1.setId("issue-1");
		Issue issue2 = new Issue();
		issue2.setId("issue-2");

		PullRequestIssue prIssue1 = new PullRequestIssue(pr1, issue1);
		PullRequestIssue prIssue2 = new PullRequestIssue(pr2, issue2);

		when(releaseService.checkIfReleaseExists(releaseId)).thenReturn(release);
		when(releasePullRequestRepository.findAllByRelease_Id(releaseId)).thenReturn(List.of(relPr1, relPr2));
		when(pullRequestIssueRepository.findAllByPullRequest_Id("pr-1")).thenReturn(List.of(prIssue1));
		when(pullRequestIssueRepository.findAllByPullRequest_Id("pr-2")).thenReturn(List.of(prIssue2));

		Set<Issue> result = helperService.getIssuesByReleaseId(releaseId);

		assertEquals(2, result.size());
		assertTrue(result.contains(issue1));
		assertTrue(result.contains(issue2));
	}

	@Test
	public void getIssuesByReleaseId_releaseNotFound_throwsException() throws ReleaseNotFoundException {
		String releaseId = "notfound";
		when(releaseService.checkIfReleaseExists(releaseId)).thenThrow(new ReleaseNotFoundException("not found", null));

		assertThrows(ReleaseNotFoundException.class, () -> helperService.getIssuesByReleaseId(releaseId));
		verify(releaseService).checkIfReleaseExists(releaseId);
		verifyNoMoreInteractions(releasePullRequestRepository, pullRequestIssueRepository);
	}

	@Test
	public void getIssuesByReleaseId_noPullRequests_returnsEmptySet() throws Exception {
		String releaseId = UUID.randomUUID().toString();
		Release release = new Release();
		release.setId(releaseId);

		when(releaseService.checkIfReleaseExists(releaseId)).thenReturn(release);
		when(releasePullRequestRepository.findAllByRelease_Id(releaseId)).thenReturn(Collections.emptyList());

		Set<Issue> result = helperService.getIssuesByReleaseId(releaseId);
		assertNotNull(result);
		assertTrue(result.isEmpty());

		verify(pullRequestIssueRepository, never()).findAllByPullRequest_Id(any());
	}

	@Test
	public void getIssuesByReleaseId_pullRequestHasNoIssues_returnsEmptySet() throws Exception {
		String releaseId = UUID.randomUUID().toString();
		Release release = new Release();
		release.setId(releaseId);

		PullRequest pr1 = new PullRequest();
		pr1.setId("pr-1");
		ReleasePullRequest relPr1 = new ReleasePullRequest(release, pr1);

		when(releaseService.checkIfReleaseExists(releaseId)).thenReturn(release);
		when(releasePullRequestRepository.findAllByRelease_Id(releaseId)).thenReturn(List.of(relPr1));
		when(pullRequestIssueRepository.findAllByPullRequest_Id("pr-1")).thenReturn(Collections.emptyList());

		Set<Issue> result = helperService.getIssuesByReleaseId(releaseId);
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	public void getIssuesByReleaseId_duplicateIssuesFromMultiplePRs_returnsUnique() throws Exception {
		String releaseId = UUID.randomUUID().toString();
		Release release = new Release();
		release.setId(releaseId);

		PullRequest pr1 = new PullRequest();
		pr1.setId("pr-1");
		PullRequest pr2 = new PullRequest();
		pr2.setId("pr-2");
		ReleasePullRequest relPr1 = new ReleasePullRequest(release, pr1);
		ReleasePullRequest relPr2 = new ReleasePullRequest(release, pr2);

		Issue issue = new Issue();
		issue.setId("issue-1");
		PullRequestIssue prIssue1 = new PullRequestIssue(pr1, issue);
		PullRequestIssue prIssue2 = new PullRequestIssue(pr2, issue);

		when(releaseService.checkIfReleaseExists(releaseId)).thenReturn(release);
		when(releasePullRequestRepository.findAllByRelease_Id(releaseId)).thenReturn(List.of(relPr1, relPr2));
		when(pullRequestIssueRepository.findAllByPullRequest_Id("pr-1")).thenReturn(List.of(prIssue1));
		when(pullRequestIssueRepository.findAllByPullRequest_Id("pr-2")).thenReturn(List.of(prIssue2));

		Set<Issue> result = helperService.getIssuesByReleaseId(releaseId);
		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue(result.contains(issue));
	}
}
