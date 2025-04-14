package org.frankframework.insights.release;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.*;

import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.commit.Commit;
import org.frankframework.insights.common.entityconnection.branchcommit.BranchCommit;
import org.frankframework.insights.common.entityconnection.branchcommit.BranchCommitRepository;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequest;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequestRepository;
import org.frankframework.insights.common.entityconnection.releasecommit.ReleaseCommitRepository;
import org.frankframework.insights.common.entityconnection.releasepullrequest.ReleasePullRequestRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubClientException;
import org.frankframework.insights.github.GitHubRepositoryStatisticsDTO;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.frankframework.insights.pullrequest.PullRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReleaseServiceTest {

	@Mock
	private GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;

	@Mock
	private GitHubClient gitHubClient;

	@Mock
	private Mapper mapper;

	@Mock
	private ReleaseRepository releaseRepository;

	@Mock
	private BranchService branchService;

	@Mock
	private ReleaseCommitRepository releaseCommitRepository;

	@Mock
	private ReleasePullRequestRepository releasePullRequestRepository;

	@Mock
	private BranchCommitRepository branchCommitRepository;

	@Mock
	private BranchPullRequestRepository branchPullRequestRepository;

	@InjectMocks
	private ReleaseService releaseService;

	private ReleaseDTO mockReleaseDTO;
	private Release mockRelease;
	private Branch mockBranch;
	private Commit mockCommit;
	private PullRequest mockPullRequest;

	@BeforeEach
	public void setUp() {
		mockCommit = new Commit();
		mockCommit.setSha("sha123");
		mockCommit.setCommittedDate(OffsetDateTime.now().minusDays(2));

		ReleaseTagCommitDTO tagCommitDTO = new ReleaseTagCommitDTO();
		tagCommitDTO.setCommitSha("sha123");

		mockReleaseDTO = new ReleaseDTO();
		mockReleaseDTO.setTagName("v1.0");
		mockReleaseDTO.setTagCommit(tagCommitDTO);
		mockReleaseDTO.setPublishedAt(OffsetDateTime.now());

		mockBranch = new Branch();
		mockBranch.setName("main");
		mockBranch.setId(UUID.randomUUID().toString());

		mockPullRequest = new PullRequest();
		mockPullRequest.setId(UUID.randomUUID().toString());
		mockPullRequest.setMergedAt(OffsetDateTime.now().minusDays(1));

		mockRelease = new Release();
		mockRelease.setTagName("v1.0");
		mockRelease.setCommitSha("sha123");
		mockRelease.setPublishedAt(mockReleaseDTO.getPublishedAt());
		mockRelease.setBranch(mockBranch);

		GitHubRepositoryStatisticsDTO statisticsDTO = mock(GitHubRepositoryStatisticsDTO.class);
		when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
				.thenReturn(statisticsDTO);
	}

	@Test
	public void should_NotInjectReleases_When_DatabaseIsUpToDate() throws Exception {
		when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubReleaseCount())
				.thenReturn(1);
		when(releaseRepository.count()).thenReturn(1L);

		releaseService.injectReleases();

		verify(gitHubClient, never()).getReleases();
		verify(releaseRepository, never()).saveAll(anySet());
	}

	@Test
	public void should_InjectReleases_When_DatabaseIsEmpty() throws Exception {
		when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubReleaseCount())
				.thenReturn(1);
		when(releaseRepository.count()).thenReturn(0L);
		when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO));
		when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));
		when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(mockRelease);
		when(branchService.doesBranchContainCommit(any(), eq("sha123"))).thenReturn(true);
		when(branchCommitRepository.findAllByBranch_Id(mockBranch.getId())).thenReturn(Set.of(new BranchCommit(mockBranch, mockCommit)));
		when(branchPullRequestRepository.findAllByBranch_Id(mockBranch.getId())).thenReturn(Set.of(new BranchPullRequest(mockBranch, mockPullRequest)));

		releaseService.injectReleases();

		verify(releaseRepository, times(1)).saveAll(anySet());
	}

	@Test
	public void should_NotInjectReleases_When_NoMatchingBranches() throws Exception {
		when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubReleaseCount())
				.thenReturn(1);
		when(releaseRepository.count()).thenReturn(0L);
		when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO));

		releaseService.injectReleases();

		verify(releaseRepository, never()).saveAll(anySet());
	}

	@Test
	public void should_ThrowInjectionException_When_GitHubFails() throws GitHubClientException {
		when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubReleaseCount())
				.thenReturn(1);
		when(releaseRepository.count()).thenReturn(0L);
		when(gitHubClient.getReleases()).thenThrow(new GitHubClientException("GitHub API error", null));

		assertThrows(ReleaseInjectionException.class, () -> releaseService.injectReleases());
	}

	@Test
	public void should_CorrectlyAssignNewCommitsToReleases() throws Exception {
		Commit newCommit = new Commit();
		newCommit.setSha("sha124");
		newCommit.setCommittedDate(OffsetDateTime.now().minusHours(5));

		when(branchCommitRepository.findAllByBranch_Id(mockBranch.getId())).thenReturn(
				Set.of(new BranchCommit(mockBranch, mockCommit), new BranchCommit(mockBranch, newCommit)));

		when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubReleaseCount())
				.thenReturn(1);
		when(releaseRepository.count()).thenReturn(0L);
		when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO));
		when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));
		when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(mockRelease);
		when(branchService.doesBranchContainCommit(any(), anyString())).thenReturn(true);
		when(branchPullRequestRepository.findAllByBranch_Id(mockBranch.getId())).thenReturn(Set.of());

		releaseService.injectReleases();

		verify(releaseRepository, times(1)).saveAll(anySet());
	}

	@Test
	public void should_AssignPullRequestsToRelease() throws Exception {
		when(branchPullRequestRepository.findAllByBranch_Id(mockBranch.getId())).thenReturn(
				Set.of(new BranchPullRequest(mockBranch, mockPullRequest)));

		when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubReleaseCount())
				.thenReturn(1);
		when(releaseRepository.count()).thenReturn(0L);
		when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO));

		releaseService.injectReleases();

		assertEquals(1, branchPullRequestRepository.findAllByBranch_Id(mockBranch.getId()).size());
	}

	@Test
	public void should_SaveReleaseCommits_When_RelevantCommitsExist() throws Exception {
		Commit commit2 = new Commit();
		commit2.setSha("sha456");
		commit2.setCommittedDate(OffsetDateTime.now().minusDays(1));

		BranchCommit branchCommit1 = new BranchCommit(mockBranch, mockCommit);
		BranchCommit branchCommit2 = new BranchCommit(mockBranch, commit2);

		when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubReleaseCount())
				.thenReturn(1);
		when(releaseRepository.count()).thenReturn(0L);
		when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO));
		when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));
		when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(mockRelease);
		when(branchService.doesBranchContainCommit(any(), anyString())).thenReturn(true);
		when(branchCommitRepository.findAllByBranch_Id(mockBranch.getId()))
				.thenReturn(Set.of(branchCommit1, branchCommit2));
		when(branchPullRequestRepository.findAllByBranch_Id(mockBranch.getId()))
				.thenReturn(Set.of());

		releaseService.injectReleases();

		verify(releaseCommitRepository, times(1)).saveAll(argThat(commits -> commits instanceof Collection && ((Collection<?>) commits).size() == 2));
	}

	@Test
	public void should_SaveReleasePullRequests_When_RelevantPRsExist() throws Exception {
		PullRequest pr2 = new PullRequest();
		pr2.setId(UUID.randomUUID().toString());
		pr2.setMergedAt(OffsetDateTime.now().minusHours(12));

		BranchPullRequest branchPR1 = new BranchPullRequest(mockBranch, mockPullRequest);
		BranchPullRequest branchPR2 = new BranchPullRequest(mockBranch, pr2);

		when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubReleaseCount())
				.thenReturn(1);
		when(releaseRepository.count()).thenReturn(0L);
		when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO));
		when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));
		when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(mockRelease);
		when(branchService.doesBranchContainCommit(any(), anyString())).thenReturn(true);
		when(branchCommitRepository.findAllByBranch_Id(mockBranch.getId()))
				.thenReturn(Set.of());
		when(branchPullRequestRepository.findAllByBranch_Id(mockBranch.getId()))
				.thenReturn(Set.of(branchPR1, branchPR2));

		releaseService.injectReleases();

		verify(releasePullRequestRepository, times(1)).saveAll(argThat(prs -> prs instanceof Collection && ((Collection<?>) prs).size() == 2));
	}
}
