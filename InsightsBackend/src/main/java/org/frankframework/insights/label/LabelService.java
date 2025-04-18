package org.frankframework.insights.label;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabel;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabelRepository;
import org.frankframework.insights.common.entityconnection.pullrequestissue.PullRequestIssue;
import org.frankframework.insights.common.entityconnection.pullrequestissue.PullRequestIssueRepository;
import org.frankframework.insights.common.entityconnection.releasepullrequest.ReleasePullRequest;
import org.frankframework.insights.common.entityconnection.releasepullrequest.ReleasePullRequestRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.mapper.MappingException;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.frankframework.insights.issue.Issue;
import org.frankframework.insights.pullrequest.PullRequest;
import org.frankframework.insights.release.Release;
import org.frankframework.insights.release.ReleaseNotFoundException;
import org.frankframework.insights.release.ReleaseService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LabelService {

    private final GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;

    private final GitHubClient gitHubClient;

    private final Mapper mapper;

    private final LabelRepository labelRepository;
    private final ReleaseService releaseService;
	private final ReleasePullRequestRepository releasePullRequestRepository;
	private final PullRequestIssueRepository pullRequestIssueRepository;
	private final IssueLabelRepository issueLabelRepository;

	public LabelService(
			GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
			GitHubClient gitHubClient,
			Mapper mapper,
			LabelRepository labelRepository,
			ReleaseService releaseService,
			ReleasePullRequestRepository releasePullRequestRepository,
			PullRequestIssueRepository pullRequestIssueRepository,
			IssueLabelRepository issueLabelRepository) {
        this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.labelRepository = labelRepository;
        this.releaseService = releaseService;
		this.releasePullRequestRepository = releasePullRequestRepository;
		this.pullRequestIssueRepository = pullRequestIssueRepository;
		this.issueLabelRepository = issueLabelRepository;
	}

    public void injectLabels() throws LabelInjectionException {
        if (gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubLabelCount()
                == labelRepository.count()) {
            log.info("Labels already found in the in database");
            return;
        }

        try {
            log.info("Amount of labels found in database: {}", labelRepository.count());
            log.info(
                    "Amount of labels found in GitHub: {}",
                    gitHubRepositoryStatisticsService
                            .getGitHubRepositoryStatisticsDTO()
                            .getGitHubLabelCount());

            log.info("Start injecting GitHub labels");
            Set<LabelDTO> labelDTOs = gitHubClient.getLabels();
            Set<Label> labels = mapper.toEntity(labelDTOs, Label.class);
            saveLabels(labels);
        } catch (Exception e) {
            throw new LabelInjectionException("Error while injecting GitHub labels", e);
        }
    }

    public Set<LabelResponse> getHighlightsByReleaseId(String releaseId)
            throws ReleaseNotFoundException, MappingException {
		Release release = releaseService.checkIfReleaseExists(releaseId);

		Set<PullRequest> releasePullRequests =
				releasePullRequestRepository.findAllByRelease_Id(release.getId()).stream()
						.map(ReleasePullRequest::getPullRequest)
						.collect(Collectors.toSet());

		Set<Issue> releaseIssues = releasePullRequests.stream()
				.flatMap(releasePullRequest ->
						pullRequestIssueRepository.findAllByPullRequest_Id(releasePullRequest.getId()).stream()
								.map(PullRequestIssue::getIssue))
				.collect(Collectors.toSet());

		Set<Label> releaseLabels = releaseIssues.stream()
				.flatMap(issue -> issueLabelRepository
						.findAllByIssue_Id(issue.getId()).stream()
						.map(IssueLabel::getLabel))
				.collect(Collectors.toSet());

        return mapper.toDTO(releaseLabels, LabelResponse.class);
    }

    private void saveLabels(Set<Label> labels) {
        List<Label> savedLabels = labelRepository.saveAll(labels);
        log.info("Successfully saved {} labels", savedLabels.size());
    }
}
