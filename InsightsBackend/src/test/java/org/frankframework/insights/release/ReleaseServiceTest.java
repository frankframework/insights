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
	private BranchCommit mockBranchCommit;
	private BranchPullRequest mockBranchPullRequest;

	@BeforeEach
	public void setUp() {
		Commit mockCommit = new Commit();
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

		PullRequest mockPullRequest = new PullRequest();
		mockPullRequest.setId(UUID.randomUUID().toString());
		mockPullRequest.setMergedAt(OffsetDateTime.now().minusDays(1));

		mockBranchCommit = new BranchCommit(mockBranch, mockCommit);
		mockBranchPullRequest = new BranchPullRequest(mockBranch, mockPullRequest);

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
		when(branchService.doesBranchContainCommit(any(), any(), eq("sha123"))).thenReturn(true);

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
	public void should_ConnectBranchToRelease_when_BranchContainsReleaseCommit() throws Exception {
		when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubReleaseCount())
				.thenReturn(1);
		when(releaseRepository.count()).thenReturn(0L);
		when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO));
		when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));
		when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(mockRelease);
		when(branchService.doesBranchContainCommit(any(), any(), anyString())).thenReturn(true);

		releaseService.injectReleases();

		verify(releaseRepository, times(1)).saveAll(anySet());
	}

	@Test
	public void should_NotConnectBranchToRelease_when_BranchDoesNotContainsReleaseCommit() throws Exception {
		when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubReleaseCount())
				.thenReturn(1);
		when(releaseRepository.count()).thenReturn(0L);
		when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO));
		when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));
		when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(mockRelease);
		when(branchService.doesBranchContainCommit(any(), any(), anyString())).thenReturn(false);

		releaseService.injectReleases();

		verify(releaseRepository, never()).saveAll(anySet());
	}

	@Test
	public void should_AssignCommitsToReleases_when_CommitIsMadeInThisRelease() throws GitHubClientException, ReleaseInjectionException {
		when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubReleaseCount())
				.thenReturn(1);
		when(releaseRepository.count()).thenReturn(0L);
		when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO));
		when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));
		when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(mockRelease);
		when(branchService.doesBranchContainCommit(any(), any(), eq("sha123"))).thenReturn(true);
		when(branchService.getBranchCommitsByBranches(anyList())).thenReturn(new HashMap<>() {{ put(mockBranch.getId(), Set.of(mockBranchCommit)); }});

		releaseService.injectReleases();

		verify(releaseCommitRepository, times(1)).saveAll(anySet());
	}

	@Test
	public void should_AssignPullRequestsToReleases_when_PullRequestIsMadeInThisRelease() throws GitHubClientException, ReleaseInjectionException {
		when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubReleaseCount())
				.thenReturn(1);
		when(releaseRepository.count()).thenReturn(0L);
		when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO));
		when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));
		when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(mockRelease);
		when(branchService.doesBranchContainCommit(any(), any(), eq("sha123"))).thenReturn(true);
		when(branchService.getBranchPullRequestsByBranches(anyList())).thenReturn(new HashMap<>() {{ put(mockBranch.getId(), Set.of(mockBranchPullRequest)); }});

		releaseService.injectReleases();

		verify(releasePullRequestRepository, times(1)).saveAll(anySet());
	}
}
