package org.frankframework.insights.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.frankframework.insights.commit.Commit;
import org.frankframework.insights.common.configuration.GitHubProperties;
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
class BranchServiceTest {

    @Mock
    private GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private Mapper branchMapper;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private GitHubProperties gitHubProperties;

    @InjectMocks
    private BranchService branchService;

    private BranchDTO mockBranchDTO;
    private Branch mockBranch;
    private Commit mockCommit;
    private GitHubRepositoryStatisticsDTO mockGitHubRepositoryStatisticsDTO;

    @BeforeEach
    void setUp() {
        mockBranchDTO = new BranchDTO();
        mockBranchDTO.setName("release/v1.0.0");

        mockBranch = new Branch();
        mockBranch.setName("release/v1.0.0");

        mockCommit = new Commit();
        mockCommit.setOid("sha123");

        mockBranch.setCommits(new HashSet<>());
        mockBranch.getCommits().add(mockCommit);

        mockGitHubRepositoryStatisticsDTO = mock(GitHubRepositoryStatisticsDTO.class);

        branchService = new BranchService(
                gitHubRepositoryStatisticsService, gitHubClient, branchMapper, branchRepository, gitHubProperties);
    }

    @Test
    public void should_InjectBranches_when_BranchesNotFoundInDatabase()
            throws BranchInjectionException, GitHubClientException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(mockGitHubRepositoryStatisticsDTO);
        when(mockGitHubRepositoryStatisticsDTO.getGitHubBranchCount(eq(Collections.emptyList()))).thenReturn(1);
        when(branchRepository.count()).thenReturn(0L);
        when(gitHubClient.getBranches()).thenReturn(Set.of(mockBranchDTO));

        branchService.injectBranches();

        verify(branchRepository, times(1)).saveAll(anySet());
    }

    @Test
    public void should_NotInjectBranches_when_BranchesAlreadyExistInDatabase()
            throws BranchInjectionException, GitHubClientException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(mockGitHubRepositoryStatisticsDTO);
        when(mockGitHubRepositoryStatisticsDTO.getGitHubBranchCount(eq(Collections.emptyList()))).thenReturn(10);
        when(branchRepository.count()).thenReturn(10L);

        branchService.injectBranches();

        verify(gitHubClient, times(0)).getBranches();
        verify(branchRepository, times(0)).saveAll(anySet());
    }

    @Test
    public void should_ThrowBranchInjectionException_when_GitHubClientThrowsException() throws GitHubClientException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(mockGitHubRepositoryStatisticsDTO);
        when(mockGitHubRepositoryStatisticsDTO.getGitHubBranchCount(eq(Collections.emptyList()))).thenReturn(1);
        when(branchRepository.count()).thenReturn(0L);
        when(gitHubClient.getBranches()).thenThrow(GitHubClientException.class);

        assertThrows(BranchInjectionException.class, () -> branchService.injectBranches());
    }

    @Test
    public void should_InjectBranches_when_BranchNamesContainRegexes()
            throws BranchInjectionException, GitHubClientException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(mockGitHubRepositoryStatisticsDTO);
        when(mockGitHubRepositoryStatisticsDTO.getGitHubBranchCount(eq(Collections.emptyList()))).thenReturn(1);
        when(branchRepository.count()).thenReturn(0L);
        when(gitHubClient.getBranches()).thenReturn(Set.of(mockBranchDTO));

        branchService.injectBranches();

        verify(branchRepository, times(1)).saveAll(anySet());
    }

    @Test
    public void should_NotInjectBranches_when_BranchNamesDoNotContainRegexes()
            throws BranchInjectionException, GitHubClientException {
        BranchDTO newBranchDTO = new BranchDTO();
        newBranchDTO.setName("feature/branch");

        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(mockGitHubRepositoryStatisticsDTO);
        when(mockGitHubRepositoryStatisticsDTO.getGitHubBranchCount(eq(Collections.emptyList()))).thenReturn(1);
        when(branchRepository.count()).thenReturn(0L);
        when(gitHubClient.getBranches()).thenReturn(Set.of(newBranchDTO));

        branchService.injectBranches();

        verify(branchRepository, times(0)).saveAll(anySet());
    }

    @Test
    public void should_CheckIfBranchContainsCommit_when_BranchContainsCommit() {
        boolean containsCommit = branchService.doesBranchContainCommit(mockBranch, mockCommit.getOid());

        assertTrue(containsCommit);
    }

    @Test
    public void should_CheckIfBranchContainsCommit_when_BranchDoesNotContainCommit() {
        Commit newCommit = new Commit();
        newCommit.setOid("sha456");

        boolean containsCommit = branchService.doesBranchContainCommit(mockBranch, newCommit.getOid());

        assertFalse(containsCommit);
    }

    @Test
    public void should_SaveBranches() {
        branchService.saveBranches(Set.of(mockBranch));

        verify(branchRepository, times(1)).saveAll(anySet());
    }

    @Test
    public void should_GetAllBranches() {
        List<Branch> branches = List.of(mockBranch);
        when(branchRepository.findAll()).thenReturn(branches);

        List<Branch> result = branchService.getAllBranches();

        assertEquals(branches, result);
    }
}
