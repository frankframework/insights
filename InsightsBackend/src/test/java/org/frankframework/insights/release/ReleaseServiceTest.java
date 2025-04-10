package org.frankframework.insights.release;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.*;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.commit.Commit;
import org.frankframework.insights.common.entityconnection.ReleasePullRequest;
import org.frankframework.insights.common.entityconnection.branchcommit.BranchCommit;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequest;
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
        mockBranch.setId(String.valueOf(UUID.randomUUID()));

        BranchCommit branchCommit = new BranchCommit(mockBranch, mockCommit);
        mockBranch.setBranchCommits(Set.of(branchCommit));

        mockPullRequest = new PullRequest();
        mockPullRequest.setId(String.valueOf(UUID.randomUUID()));
        mockPullRequest.setMergedAt(OffsetDateTime.now().minusDays(1));

        BranchPullRequest branchPullRequest = new BranchPullRequest(mockBranch, mockPullRequest);
        mockBranch.setBranchPullRequests(Set.of(branchPullRequest));

        mockRelease = new Release();
        mockRelease.setTagName("v1.0");
        mockRelease.setCommitSha("sha123");
        mockRelease.setPublishedAt(mockReleaseDTO.getPublishedAt());
        mockRelease.setBranch(mockBranch);
        mockRelease.setReleaseCommits(new HashSet<>());
        mockRelease.setReleasePullRequests(new HashSet<>());

        GitHubRepositoryStatisticsDTO statisticsDTO = mock(GitHubRepositoryStatisticsDTO.class);
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statisticsDTO);
    }

    @Test
    public void should_NotInjectReleases_When_DatabaseIsUpToDate()
            throws ReleaseInjectionException, GitHubClientException {
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
    public void should_InjectReleases_When_DatabaseIsEmpty() throws ReleaseInjectionException, GitHubClientException {
        when(gitHubRepositoryStatisticsService
                        .getGitHubRepositoryStatisticsDTO()
                        .getGitHubReleaseCount())
                .thenReturn(1);
        when(releaseRepository.count()).thenReturn(0L);
        when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO));
        when(branchService.getBranchesWithCommits()).thenReturn(List.of(mockBranch));
        when(branchService.getBranchesWithPullRequests(List.of(mockBranch))).thenReturn(List.of(mockBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(mockRelease);
        when(branchService.doesBranchContainCommit(any(), eq("sha123"))).thenReturn(true);

        releaseService.injectReleases();

        verify(releaseRepository, times(1)).saveAll(anySet());
    }

    @Test
    public void should_NotInjectReleases_When_NoMatchingBranches()
            throws ReleaseInjectionException, GitHubClientException {
        when(gitHubRepositoryStatisticsService
                        .getGitHubRepositoryStatisticsDTO()
                        .getGitHubReleaseCount())
                .thenReturn(1);
        when(releaseRepository.count()).thenReturn(0L);
        when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO));
        when(branchService.getBranchesWithCommits()).thenReturn(List.of(mockBranch));
        when(branchService.getBranchesWithPullRequests(List.of(mockBranch))).thenReturn(List.of(mockBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(mockRelease);
        when(branchService.doesBranchContainCommit(any(), eq("sha123"))).thenReturn(false);

        releaseService.injectReleases();

        verify(releaseRepository, never()).saveAll(anySet());
    }

    @Test
    public void should_ThrowInjectionException_When_GitHubFails() throws GitHubClientException {
        when(gitHubRepositoryStatisticsService
                        .getGitHubRepositoryStatisticsDTO()
                        .getGitHubReleaseCount())
                .thenReturn(1);
        when(releaseRepository.count()).thenReturn(0L);
        when(gitHubClient.getReleases()).thenThrow(new GitHubClientException("GitHub API error", null));

        assertThrows(ReleaseInjectionException.class, () -> releaseService.injectReleases());
    }

    @Test
    public void should_CorrectlyAssignNewCommitsToReleases() throws ReleaseInjectionException, GitHubClientException {
        Commit newCommit = new Commit();
        newCommit.setSha("sha124");
        newCommit.setCommittedDate(OffsetDateTime.now().minusHours(5));

        Set<BranchCommit> branchCommits = new HashSet<>();
        branchCommits.add(new BranchCommit(mockBranch, mockCommit));
        branchCommits.add(new BranchCommit(mockBranch, newCommit));
        mockBranch.setBranchCommits(branchCommits);

        when(gitHubRepositoryStatisticsService
                        .getGitHubRepositoryStatisticsDTO()
                        .getGitHubReleaseCount())
                .thenReturn(2);
        when(releaseRepository.count()).thenReturn(0L);
        when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO));
        when(branchService.getBranchesWithCommits()).thenReturn(List.of(mockBranch));
        when(branchService.getBranchesWithPullRequests(List.of(mockBranch))).thenReturn(List.of(mockBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(mockRelease);
        when(branchService.doesBranchContainCommit(any(), anyString())).thenReturn(true);

        releaseService.injectReleases();

        verify(releaseRepository, times(1)).saveAll(anySet());

        assertTrue(mockRelease.getReleaseCommits().stream()
                .anyMatch(releaseCommit -> releaseCommit.getCommit().getSha().equals("sha124")));
    }

    @Test
    public void should_AssignPullRequestsToRelease() throws Exception {
        when(gitHubRepositoryStatisticsService
                        .getGitHubRepositoryStatisticsDTO()
                        .getGitHubReleaseCount())
                .thenReturn(2);
        when(releaseRepository.count()).thenReturn(0L);
        when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO));
        when(branchService.getBranchesWithCommits()).thenReturn(List.of(mockBranch));
        when(branchService.getBranchesWithPullRequests(List.of(mockBranch))).thenReturn(List.of(mockBranch));
        when(branchService.doesBranchContainCommit(any(), eq("sha123"))).thenReturn(true);
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(mockRelease);
        when(releaseRepository.saveAll(anySet())).thenAnswer(invocation -> new ArrayList<>(invocation.getArgument(0)));

        releaseService.injectReleases();

        assertEquals(1, mockRelease.getReleaseCommits().size());
        assertEquals(1, mockRelease.getReleasePullRequests().size());

        ReleasePullRequest releasePR =
                mockRelease.getReleasePullRequests().iterator().next();
        assertEquals(mockPullRequest.getId(), releasePR.getPullRequest().getId());
    }

    @Test
    public void should_HandleMultipleReleasesOnSameBranch() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();

        Commit earlierCommit = new Commit();
        earlierCommit.setSha("sha122");
        earlierCommit.setCommittedDate(now.minusDays(4));

        Commit laterCommit = new Commit();
        laterCommit.setSha("sha124");
        laterCommit.setCommittedDate(now.minusDays(1));

        BranchCommit bc1 = new BranchCommit(mockBranch, earlierCommit);
        BranchCommit bc2 = new BranchCommit(mockBranch, laterCommit);
        mockBranch.setBranchCommits(Set.of(bc1, bc2));

        ReleaseDTO releaseDTO1 = new ReleaseDTO();
        releaseDTO1.setTagName("v0.9");
        releaseDTO1.setTagCommit(new ReleaseTagCommitDTO("sha122"));
        releaseDTO1.setPublishedAt(now.minusDays(2));

        ReleaseDTO releaseDTO2 = new ReleaseDTO();
        releaseDTO2.setTagName("v1.0");
        releaseDTO2.setTagCommit(new ReleaseTagCommitDTO("sha124"));
        releaseDTO2.setPublishedAt(now);

        Release release1 = new Release();
        release1.setTagName("v0.9");
        release1.setCommitSha("sha122");
        release1.setPublishedAt(releaseDTO1.getPublishedAt());
        release1.setBranch(mockBranch);
        release1.setReleaseCommits(new HashSet<>());
        release1.setReleasePullRequests(new HashSet<>());

        Release release2 = new Release();
        release2.setTagName("v1.0");
        release2.setCommitSha("sha124");
        release2.setPublishedAt(releaseDTO2.getPublishedAt());
        release2.setBranch(mockBranch);
        release2.setReleaseCommits(new HashSet<>());
        release2.setReleasePullRequests(new HashSet<>());

        when(gitHubRepositoryStatisticsService
                        .getGitHubRepositoryStatisticsDTO()
                        .getGitHubReleaseCount())
                .thenReturn(2);
        when(releaseRepository.count()).thenReturn(0L);
        when(gitHubClient.getReleases()).thenReturn(Set.of(releaseDTO1, releaseDTO2));
        when(branchService.getBranchesWithCommits()).thenReturn(List.of(mockBranch));
        when(branchService.getBranchesWithPullRequests(any())).thenReturn(List.of(mockBranch));
        when(branchService.doesBranchContainCommit(mockBranch, "sha122")).thenReturn(true);
        when(branchService.doesBranchContainCommit(mockBranch, "sha124")).thenReturn(true);
        when(mapper.toEntity(releaseDTO1, Release.class)).thenReturn(release1);
        when(mapper.toEntity(releaseDTO2, Release.class)).thenReturn(release2);
        when(releaseRepository.saveAll(anySet())).thenAnswer(invocation -> new ArrayList<>(invocation.getArgument(0)));

        releaseService.injectReleases();

        assertEquals(1, release1.getReleaseCommits().size());
        assertEquals(1, release2.getReleaseCommits().size());

        // Ensure that release2 does NOT include commits from before release1
        assertTrue(release2.getReleaseCommits().stream()
                .noneMatch(rc -> rc.getCommit().getSha().equals("sha122")));
    }
}
