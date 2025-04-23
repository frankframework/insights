package org.frankframework.insights.release;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.*;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.commit.Commit;
import org.frankframework.insights.common.entityconnection.branchcommit.BranchCommit;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequest;
import org.frankframework.insights.common.entityconnection.releasecommit.ReleaseCommitRepository;
import org.frankframework.insights.common.entityconnection.releasepullrequest.ReleasePullRequestRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.*;
import org.frankframework.insights.pullrequest.PullRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReleaseServiceTest {

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

    @InjectMocks
    private ReleaseService releaseService;

    private ReleaseDTO mockReleaseDTO;
    private Release mockRelease;
    private Branch mockBranch;
    private BranchCommit mockBranchCommit;
    private BranchPullRequest mockBranchPullRequest;
    private Commit mockCommit;
    private PullRequest mockPullRequest;

    @BeforeEach
    public void setUp() {
        mockCommit = new Commit();
        mockCommit.setSha("sha123");
        mockCommit.setCommittedDate(OffsetDateTime.now().plusDays(2));

        mockPullRequest = new PullRequest();
        mockPullRequest.setId(UUID.randomUUID().toString());
        mockPullRequest.setMergedAt(OffsetDateTime.now().plusDays(1));

        mockBranch = new Branch();
        mockBranch.setId(UUID.randomUUID().toString());
        mockBranch.setName("main");

        mockBranchCommit = new BranchCommit(mockBranch, mockCommit);
        mockBranchPullRequest = new BranchPullRequest(mockBranch, mockPullRequest);

        mockReleaseDTO = new ReleaseDTO();
        mockReleaseDTO.setTagName("v1.0");
        mockReleaseDTO.setPublishedAt(OffsetDateTime.now());
        mockReleaseDTO.setCommitSha("sha123");
        ReleaseTagCommitDTO releaseTagCommitDTO = new ReleaseTagCommitDTO();
        releaseTagCommitDTO.setCommitSha("sha123");
        mockReleaseDTO.setTagCommit(releaseTagCommitDTO);

        mockRelease = new Release();
        mockRelease.setTagName("v1.0");
        mockRelease.setCommitSha(mockCommit.getSha());
        mockRelease.setPublishedAt(mockReleaseDTO.getPublishedAt());
        mockRelease.setBranch(mockBranch);

        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(mock(GitHubRepositoryStatisticsDTO.class));
    }

    @Test
    public void shouldSkipInjectionWhenDatabaseIsUpToDate() throws Exception {
        when(gitHubRepositoryStatisticsService
                        .getGitHubRepositoryStatisticsDTO()
                        .getGitHubReleaseCount())
                .thenReturn(1);
        when(releaseRepository.count()).thenReturn(1L);

        releaseService.injectReleases();

        verify(gitHubClient, never()).getReleases();
        verify(releaseRepository, never()).saveAll(anySet());
    }

    @Test
    public void should_InjectReleases_when_DatabaseIsEmpty() throws Exception {
        when(gitHubRepositoryStatisticsService
                        .getGitHubRepositoryStatisticsDTO()
                        .getGitHubReleaseCount())
                .thenReturn(1);
        when(releaseRepository.count()).thenReturn(0L);
        when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO));
        when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(mockRelease);
        when(branchService.getBranchCommitsByBranches(anyList()))
                .thenReturn(Map.of(mockBranch.getId(), Set.of(mockBranchCommit)));

        releaseService.injectReleases();

        verify(releaseRepository).saveAll(anySet());
    }

    @Test
    public void should_NotInjectRelease_when_NoMatchingBranches() throws Exception {
        when(gitHubRepositoryStatisticsService
                        .getGitHubRepositoryStatisticsDTO()
                        .getGitHubReleaseCount())
                .thenReturn(1);
        when(releaseRepository.count()).thenReturn(0L);
        when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO));
        when(branchService.getAllBranches()).thenReturn(Collections.emptyList());

        releaseService.injectReleases();

        verify(releaseRepository, never()).saveAll(anySet());
    }

    @Test
    public void should_ThrowInjectionException_when_GitHubFailure() throws GitHubClientException {
        when(gitHubRepositoryStatisticsService
                        .getGitHubRepositoryStatisticsDTO()
                        .getGitHubReleaseCount())
                .thenReturn(1);
        when(releaseRepository.count()).thenReturn(0L);
        when(gitHubClient.getReleases()).thenThrow(new GitHubClientException("Error", null));

        assertThrows(ReleaseInjectionException.class, () -> releaseService.injectReleases());
    }

    @Test
    public void should_AssignCommitsToRelease_when_WithinTimeframe() throws Exception {
        Release mockNewRelease = new Release();
        mockNewRelease.setTagName("v1.0");
        mockNewRelease.setCommitSha(mockCommit.getSha());
        mockNewRelease.setPublishedAt(mockReleaseDTO.getPublishedAt().plusDays(3));
        mockNewRelease.setBranch(mockBranch);

        ReleaseDTO mockNewReleaseDTO = new ReleaseDTO();
        mockNewReleaseDTO.setTagName("v1.0");
        mockNewReleaseDTO.setPublishedAt(OffsetDateTime.now());
        mockNewReleaseDTO.setCommitSha("sha123");
        ReleaseTagCommitDTO releaseTagCommitDTO = new ReleaseTagCommitDTO();
        releaseTagCommitDTO.setCommitSha("sha123");
        mockNewReleaseDTO.setTagCommit(releaseTagCommitDTO);

        when(gitHubRepositoryStatisticsService
                        .getGitHubRepositoryStatisticsDTO()
                        .getGitHubReleaseCount())
                .thenReturn(1);
        when(releaseRepository.count()).thenReturn(0L);
        when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO, mockNewReleaseDTO));
        when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class)))
                .thenReturn(mockRelease)
                .thenReturn(mockNewRelease);
        when(branchService.getBranchCommitsByBranches(anyList()))
                .thenReturn(Map.of(mockBranch.getId(), Set.of(mockBranchCommit)));
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(mockRelease, mockNewRelease));

        releaseService.injectReleases();

        verify(releaseCommitRepository).saveAll(anySet());
    }

    @Test
    public void should_AssignPullRequestsToRelease_when_WithinTimeframe() throws Exception {
        Release mockNewRelease = new Release();
        mockNewRelease.setTagName("v1.1");
        mockNewRelease.setCommitSha(mockCommit.getSha());
        mockNewRelease.setPublishedAt(mockReleaseDTO.getPublishedAt().plusDays(3));
        mockNewRelease.setBranch(mockBranch);

        ReleaseDTO mockNewReleaseDTO = new ReleaseDTO();
        mockNewReleaseDTO.setTagName("v1.1");
        mockNewReleaseDTO.setPublishedAt(OffsetDateTime.now());
        mockNewReleaseDTO.setCommitSha("sha123");
        ReleaseTagCommitDTO releaseTagCommitDTO = new ReleaseTagCommitDTO();
        releaseTagCommitDTO.setCommitSha("sha123");
        mockNewReleaseDTO.setTagCommit(releaseTagCommitDTO);

        when(gitHubRepositoryStatisticsService
                        .getGitHubRepositoryStatisticsDTO()
                        .getGitHubReleaseCount())
                .thenReturn(1);
        when(releaseRepository.count()).thenReturn(0L);
        when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO, mockNewReleaseDTO));
        when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class)))
                .thenReturn(mockRelease)
                .thenReturn(mockNewRelease);
        when(branchService.getBranchCommitsByBranches(anyList()))
                .thenReturn(Map.of(mockBranch.getId(), Set.of(mockBranchCommit)));
        when(branchService.getBranchPullRequestsByBranches(anyList()))
                .thenReturn(Map.of(mockBranch.getId(), Set.of(mockBranchPullRequest)));
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(mockRelease, mockNewRelease));

        releaseService.injectReleases();

        verify(releasePullRequestRepository).saveAll(anySet());
    }

    @Test
    public void should_NotAssignCommit_when_OutsideReleaseTimeframe() throws Exception {
        Commit outsideCommit = mockCommit;
        outsideCommit.setCommittedDate(OffsetDateTime.now().plusDays(100));
        mockBranchCommit.setCommit(outsideCommit);

        mockCommit.setCommittedDate(OffsetDateTime.now().plusDays(100));
        Release mockNewRelease = new Release();
        mockNewRelease.setTagName("v1.0");
        mockNewRelease.setCommitSha(mockCommit.getSha());
        mockNewRelease.setPublishedAt(mockReleaseDTO.getPublishedAt().plusDays(3));
        mockNewRelease.setBranch(mockBranch);

        ReleaseDTO mockNewReleaseDTO = new ReleaseDTO();
        mockNewReleaseDTO.setTagName("v1.0");
        mockNewReleaseDTO.setPublishedAt(OffsetDateTime.now());
        mockNewReleaseDTO.setCommitSha("sha123");
        ReleaseTagCommitDTO releaseTagCommitDTO = new ReleaseTagCommitDTO();
        releaseTagCommitDTO.setCommitSha("sha123");
        mockNewReleaseDTO.setTagCommit(releaseTagCommitDTO);

        when(gitHubRepositoryStatisticsService
                        .getGitHubRepositoryStatisticsDTO()
                        .getGitHubReleaseCount())
                .thenReturn(1);
        when(releaseRepository.count()).thenReturn(0L);
        when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO, mockNewReleaseDTO));
        when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class)))
                .thenReturn(mockRelease)
                .thenReturn(mockNewRelease);
        when(branchService.getBranchCommitsByBranches(anyList()))
                .thenReturn(Map.of(mockBranch.getId(), Set.of(mockBranchCommit)));
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(mockRelease, mockNewRelease));

        releaseService.injectReleases();

        verify(releaseCommitRepository, never()).saveAll(anySet());
    }

    @Test
    public void should_NotAssignPullRequest_when_OutsideReleaseTimeframe() throws Exception {
        PullRequest outsidePullRequest = mockPullRequest;
        outsidePullRequest.setMergedAt(OffsetDateTime.now().plusDays(100));
        mockBranchPullRequest.setPullRequest(outsidePullRequest);

        mockCommit.setCommittedDate(OffsetDateTime.now().plusDays(100));
        Release mockNewRelease = new Release();
        mockNewRelease.setTagName("v1.0");
        mockNewRelease.setCommitSha(mockCommit.getSha());
        mockNewRelease.setPublishedAt(mockReleaseDTO.getPublishedAt().plusDays(3));
        mockNewRelease.setBranch(mockBranch);

        ReleaseDTO mockNewReleaseDTO = new ReleaseDTO();
        mockNewReleaseDTO.setTagName("v1.0");
        mockNewReleaseDTO.setPublishedAt(OffsetDateTime.now());
        mockNewReleaseDTO.setCommitSha("sha123");
        ReleaseTagCommitDTO releaseTagCommitDTO = new ReleaseTagCommitDTO();
        releaseTagCommitDTO.setCommitSha("sha123");
        mockNewReleaseDTO.setTagCommit(releaseTagCommitDTO);

        when(gitHubRepositoryStatisticsService
                        .getGitHubRepositoryStatisticsDTO()
                        .getGitHubReleaseCount())
                .thenReturn(1);
        when(releaseRepository.count()).thenReturn(0L);
        when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO, mockNewReleaseDTO));
        when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class)))
                .thenReturn(mockRelease)
                .thenReturn(mockNewRelease);
        when(branchService.getBranchCommitsByBranches(anyList()))
                .thenReturn(Map.of(mockBranch.getId(), Set.of(mockBranchCommit)));
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(mockRelease, mockNewRelease));

        releaseService.injectReleases();

        verify(releasePullRequestRepository, never()).saveAll(anySet());
    }
}
