package org.frankframework.insights.common.helper;

import java.util.Set;
import java.util.stream.Collectors;
import org.frankframework.insights.common.entityconnection.pullrequestissue.PullRequestIssue;
import org.frankframework.insights.common.entityconnection.pullrequestissue.PullRequestIssueRepository;
import org.frankframework.insights.common.entityconnection.releasepullrequest.ReleasePullRequest;
import org.frankframework.insights.common.entityconnection.releasepullrequest.ReleasePullRequestRepository;
import org.frankframework.insights.issue.Issue;
import org.frankframework.insights.pullrequest.PullRequest;
import org.frankframework.insights.release.Release;
import org.frankframework.insights.release.ReleaseNotFoundException;
import org.frankframework.insights.release.ReleaseService;
import org.springframework.stereotype.Service;

@Service
public class ReleaseIssueHelperService {

    private final ReleaseService releaseService;
    private final ReleasePullRequestRepository releasePullRequestRepository;
    private final PullRequestIssueRepository pullRequestIssueRepository;

    public ReleaseIssueHelperService(
            ReleaseService releaseService,
            ReleasePullRequestRepository releasePullRequestRepository,
            PullRequestIssueRepository pullRequestIssueRepository) {
        this.releaseService = releaseService;
        this.releasePullRequestRepository = releasePullRequestRepository;
        this.pullRequestIssueRepository = pullRequestIssueRepository;
    }

	/**
	 * Fetches all issues associated with a given release ID.
	 * @param releaseId The ID of the release to fetch issues for
	 * @return Set of issues associated with the release
	 * @throws ReleaseNotFoundException if the release is not found
	 */
    public Set<Issue> getIssuesByReleaseId(String releaseId) throws ReleaseNotFoundException {
        Release release = releaseService.checkIfReleaseExists(releaseId);

        Set<PullRequest> releasePullRequests =
                releasePullRequestRepository.findAllByRelease_Id(release.getId()).stream()
                        .map(ReleasePullRequest::getPullRequest)
                        .collect(Collectors.toSet());

        return releasePullRequests.stream()
                .flatMap(pr -> pullRequestIssueRepository.findAllByPullRequest_Id(pr.getId()).stream()
                        .map(PullRequestIssue::getIssue))
                .collect(Collectors.toSet());
    }
}
