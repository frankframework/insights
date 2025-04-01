package org.frankframework.insights.release;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.*;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.commit.Commit;
import org.frankframework.insights.common.entityconnection.branchcommit.BranchCommit;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubClientException;
import org.frankframework.insights.github.GitHubRepositoryStatisticsDTO;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
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

    @BeforeEach
    public void setUp() {
        mockCommit = new Commit();
        mockCommit.setSha("sha123");
        mockCommit.setCommittedDate(OffsetDateTime.now().minusDays(1));

        ReleaseTagCommitDTO tagCommitDTO = new ReleaseTagCommitDTO();
        tagCommitDTO.setCommitSha("sha123");

        mockReleaseDTO = new ReleaseDTO();
        mockReleaseDTO.setTagName("v1.0");
        mockReleaseDTO.setTagCommit(tagCommitDTO);
        mockReleaseDTO.setPublishedAt(OffsetDateTime.now());

        mockBranch = new Branch();
        mockBranch.setName("main");
        mockBranch.setBranchCommits(Set.of(new BranchCommit(mockBranch, mockCommit)));

        mockRelease = new Release();
        mockRelease.setTagName("v1.0");
        mockRelease.setCommitSha("sha123");
        mockRelease.setBranch(mockBranch);
        mockRelease.setPublishedAt(OffsetDateTime.now());
        mockRelease.setReleaseCommits(new HashSet<>());

        GitHubRepositoryStatisticsDTO mockGitHubRepositoryStatisticsDTO = mock(GitHubRepositoryStatisticsDTO.class);
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(mockGitHubRepositoryStatisticsDTO);
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
        when(branchService.getAllBranchesWithCommits()).thenReturn(List.of(mockBranch));
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
        when(branchService.getAllBranchesWithCommits()).thenReturn(List.of(mockBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(mockRelease);
        when(branchService.doesBranchContainCommit(any(), eq("sha123"))).thenReturn(false);

        releaseService.injectReleases();

        verify(releaseRepository, never()).saveAll(anySet());
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
        when(branchService.getAllBranchesWithCommits()).thenReturn(List.of(mockBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(mockRelease);
        when(branchService.doesBranchContainCommit(any(), anyString())).thenReturn(true);

        releaseService.injectReleases();

        verify(releaseRepository, times(1)).saveAll(anySet());

        assertTrue(mockRelease.getReleaseCommits().stream()
                .anyMatch(releaseCommit -> releaseCommit.getCommit().getSha().equals("sha124")));
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
}
