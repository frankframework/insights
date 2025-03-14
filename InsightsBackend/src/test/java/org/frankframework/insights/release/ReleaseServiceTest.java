package org.frankframework.insights.release;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.*;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.commit.Commit;
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
    private Mapper releaseMapper;

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
    private GitHubRepositoryStatisticsDTO mockGitHubRepositoryStatisticsDTO;
    private ReleaseTagCommitDTO mockReleaseTagCommitDTO;

    @BeforeEach
    void setUp() {
        // Create mock commit
        mockCommit = new Commit();
        mockCommit.setOid("sha123");
        mockCommit.setCommittedDate(OffsetDateTime.now());

        mockReleaseTagCommitDTO = new ReleaseTagCommitDTO();
        mockReleaseTagCommitDTO.setOid("sha123");

        mockReleaseDTO = new ReleaseDTO();
        mockReleaseDTO.setTagName("v9.0");
        mockReleaseDTO.setTagCommit(mockReleaseTagCommitDTO);

        mockBranch = new Branch();
        mockBranch.setName("master");
        mockBranch.setCommits(Set.of(mockCommit));

        mockRelease = new Release();
        mockRelease.setTagName("v9.0");
        mockRelease.setOid(mockReleaseTagCommitDTO.getOid());
        mockRelease.setBranch(mockBranch);

        mockGitHubRepositoryStatisticsDTO = mock(GitHubRepositoryStatisticsDTO.class);
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(mockGitHubRepositoryStatisticsDTO);
    }


    @Test
    public void should_InjectReleases_when_DatabaseIsNotFilledYet()
            throws GitHubClientException, ReleaseInjectionException {
        when(mockGitHubRepositoryStatisticsDTO.getGitHubReleaseCount()).thenReturn(10);
        when(releaseRepository.count()).thenReturn(0L);

        releaseService.injectReleases();

        verify(gitHubClient, times(1)).getReleases();
        verify(releaseRepository, never()).saveAll(anySet());
    }

    @Test
    public void should_NotInjectReleases_when_DatabaseIsAlreadyFilled()
            throws GitHubClientException, ReleaseInjectionException {
        when(mockGitHubRepositoryStatisticsDTO.getGitHubReleaseCount()).thenReturn(10);
        when(releaseRepository.count()).thenReturn(10L);

        releaseService.injectReleases();

        verify(gitHubClient, times(0)).getReleases();
        verify(releaseRepository, times(0)).saveAll(anySet());
    }

    @Test
    public void should_InjectReleases_when_MultipleBranchesAreMatching()
            throws GitHubClientException, ReleaseInjectionException {
        Branch developmentBranch = new Branch();
        developmentBranch.setName("development");
        developmentBranch.setCommits(Set.of(mockCommit));

        when(mockGitHubRepositoryStatisticsDTO.getGitHubReleaseCount()).thenReturn(10);
        when(releaseRepository.count()).thenReturn(0L);
        when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO));
        when(branchService.getAllBranches()).thenReturn(List.of(mockBranch, developmentBranch));
        when(releaseMapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(mockRelease);
        when(branchService.doesBranchContainCommit(any(), any())).thenReturn(true);

        releaseService.injectReleases();

        verify(releaseRepository, times(1)).saveAll(anySet());
    }

    @Test
    public void should_NotInjectReleases_when_ReleaseBranchesMapIsEmpty()
            throws GitHubClientException, ReleaseInjectionException {
        when(mockGitHubRepositoryStatisticsDTO.getGitHubReleaseCount()).thenReturn(10);
        when(releaseRepository.count()).thenReturn(0L);
        when(gitHubClient.getReleases()).thenReturn(Set.of(mockReleaseDTO));
        when(branchService.getAllBranches()).thenReturn(Collections.emptyList());

        releaseService.injectReleases();

        verify(releaseRepository, times(0)).saveAll(anySet());
    }

    @Test
    public void should_ThrowInjectionException_when_GitHubIsRaisingAnError() throws GitHubClientException {
        when(mockGitHubRepositoryStatisticsDTO.getGitHubReleaseCount()).thenReturn(1);
        when(releaseRepository.count()).thenReturn(0L);
        when(gitHubClient.getReleases())
                .thenThrow(new GitHubClientException("Error fetching releases from GitHub", null));

        assertThrows(ReleaseInjectionException.class, () -> releaseService.injectReleases());
    }
}
