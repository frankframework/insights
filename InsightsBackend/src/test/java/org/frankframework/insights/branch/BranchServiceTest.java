package org.frankframework.insights.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.regex.Pattern;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BranchServiceTest {

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

    private List<String> branchProtectionRegexes;

    @BeforeEach
    void setUp() {
        mockBranchDTO = new BranchDTO();
        mockBranchDTO.setName("release/v1.0.0");

        mockBranch = new Branch();
        mockBranch.setName("release/v1.0.0");

        mockCommit = new Commit();
        mockCommit.setOid("sha123");

        mockBranch.setCommits(new HashSet<>(Set.of(mockCommit)));

        mockGitHubRepositoryStatisticsDTO = mock(GitHubRepositoryStatisticsDTO.class);

        branchProtectionRegexes = List.of("release/.*", "hotfix/.*");

        when(gitHubProperties.getBranchProtectionRegexes()).thenReturn(branchProtectionRegexes);

        branchService = new BranchService(
                gitHubRepositoryStatisticsService, gitHubClient, branchMapper, branchRepository, gitHubProperties);
    }

    @Test
    void shouldInjectBranches_whenBranchesNotFoundInDatabase() throws BranchInjectionException, GitHubClientException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(mockGitHubRepositoryStatisticsDTO);
        when(mockGitHubRepositoryStatisticsDTO.getGitHubBranchCount(branchProtectionRegexes)).thenReturn(1);
        when(branchRepository.count()).thenReturn(0L);
        when(gitHubClient.getBranches()).thenReturn(Set.of(mockBranchDTO));
        when(branchMapper.toEntity(mockBranchDTO, Branch.class)).thenReturn(mockBranch);

        branchService.injectBranches();

        verify(branchRepository, times(1)).saveAll(anySet());
    }

    @Test
    void shouldNotInjectBranches_whenBranchesAlreadyExistInDatabase() throws BranchInjectionException, GitHubClientException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(mockGitHubRepositoryStatisticsDTO);
        when(mockGitHubRepositoryStatisticsDTO.getGitHubBranchCount(branchProtectionRegexes)).thenReturn(10);
        when(branchRepository.count()).thenReturn(10L);

        branchService.injectBranches();

        verify(gitHubClient, never()).getBranches();
        verify(branchRepository, never()).saveAll(anySet());
    }

    @Test
    void shouldThrowBranchInjectionException_whenGitHubClientThrowsException() throws GitHubClientException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(mockGitHubRepositoryStatisticsDTO);
        when(mockGitHubRepositoryStatisticsDTO.getGitHubBranchCount(branchProtectionRegexes)).thenReturn(1);
        when(branchRepository.count()).thenReturn(0L);
        when(gitHubClient.getBranches()).thenThrow(GitHubClientException.class);

        assertThrows(BranchInjectionException.class, () -> branchService.injectBranches());
    }

    @Test
    void shouldInjectBranches_whenBranchNamesMatchRegexes() throws BranchInjectionException, GitHubClientException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(mockGitHubRepositoryStatisticsDTO);
        when(mockGitHubRepositoryStatisticsDTO.getGitHubBranchCount(branchProtectionRegexes)).thenReturn(1);
        when(branchRepository.count()).thenReturn(0L);
        when(gitHubClient.getBranches()).thenReturn(Set.of(mockBranchDTO));
        when(branchMapper.toEntity(mockBranchDTO, Branch.class)).thenReturn(mockBranch);

        branchService.injectBranches();

        ArgumentCaptor<Set<Branch>> captor = ArgumentCaptor.forClass(Set.class);
        verify(branchRepository, times(1)).saveAll(captor.capture());

        assertFalse(captor.getValue().isEmpty());    }

    @Test
    void shouldNotInjectBranches_whenBranchNamesDoNotMatchRegexes() throws BranchInjectionException, GitHubClientException {
        BranchDTO newBranchDTO = new BranchDTO();
        newBranchDTO.setName("feature/branch");

        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(mockGitHubRepositoryStatisticsDTO);
        when(mockGitHubRepositoryStatisticsDTO.getGitHubBranchCount(branchProtectionRegexes)).thenReturn(1);
        when(branchRepository.count()).thenReturn(0L);
        when(gitHubClient.getBranches()).thenReturn(Set.of(newBranchDTO));

        branchService.injectBranches();

        ArgumentCaptor<Set<Branch>> captor = ArgumentCaptor.forClass(Set.class);
        verify(branchRepository, times(1)).saveAll(captor.capture());

        assertTrue(captor.getValue().isEmpty());
    }

    @Test
    void shouldCheckIfBranchContainsCommit_whenBranchContainsCommit() {
        boolean containsCommit = branchService.doesBranchContainCommit(mockBranch, mockCommit.getOid());

        assertTrue(containsCommit);
    }

    @Test
    void shouldCheckIfBranchContainsCommit_whenBranchDoesNotContainCommit() {
        Commit newCommit = new Commit();
        newCommit.setOid("sha456");

        boolean containsCommit = branchService.doesBranchContainCommit(mockBranch, newCommit.getOid());

        assertFalse(containsCommit);
    }

    @Test
    void shouldSaveBranches() {
        branchService.saveBranches(Set.of(mockBranch));

        verify(branchRepository, times(1)).saveAll(anySet());
    }

    @Test
    void shouldGetAllBranches() {
        List<Branch> branches = List.of(mockBranch);
        when(branchRepository.findAll()).thenReturn(branches);

        List<Branch> result = branchService.getAllBranches();

        assertEquals(branches, result);
    }
}
