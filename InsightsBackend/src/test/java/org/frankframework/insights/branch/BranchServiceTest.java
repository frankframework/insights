package org.frankframework.insights.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.frankframework.insights.commit.Commit;
import org.frankframework.insights.common.configuration.GitHubProperties;
import org.frankframework.insights.common.entityconnection.branchcommit.BranchCommit;
import org.frankframework.insights.common.entityconnection.branchcommit.BranchCommitRepository;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequest;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequestRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubClientException;
import org.frankframework.insights.github.GitHubRepositoryStatisticsDTO;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.frankframework.insights.pullrequest.PullRequest;
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
    private BranchCommitRepository branchCommitRepository;

    @Mock
    private BranchPullRequestRepository branchPullRequestRepository;

    @Mock
    private GitHubProperties gitHubProperties;

    @InjectMocks
    private BranchService branchService;

    private BranchDTO mockBranchDTO;
    private Branch mockBranch;
    private Commit mockCommit;
    private PullRequest mockPullRequest;
    private BranchCommit mockBranchCommit;
    private BranchPullRequest mockBranchPullRequest;
    private GitHubRepositoryStatisticsDTO mockGitHubRepositoryStatisticsDTO;

    private List<String> branchProtectionRegexes;

    @BeforeEach
    public void setUp() {
        mockBranchDTO = new BranchDTO();
        mockBranchDTO.setName("release/v1.0.0");

        mockBranch = new Branch();
        mockBranch.setName("release/v1.0.0");

        mockCommit = new Commit();
        mockCommit.setSha("sha123");

        mockPullRequest = new PullRequest();
        mockPullRequest.setId(UUID.randomUUID().toString());

        mockBranchCommit = new BranchCommit(mockBranch, mockCommit);

        mockBranchPullRequest = new BranchPullRequest(mockBranch, mockPullRequest);

        mockGitHubRepositoryStatisticsDTO = mock(GitHubRepositoryStatisticsDTO.class);

        branchProtectionRegexes = List.of("release", "master");

        when(gitHubProperties.getBranchProtectionRegexes()).thenReturn(branchProtectionRegexes);

        branchService = new BranchService(
                gitHubRepositoryStatisticsService,
                gitHubClient,
                branchMapper,
                branchRepository,
                branchCommitRepository,
                gitHubProperties,
                branchPullRequestRepository);
    }

    @Test
    public void shouldInjectBranches_whenBranchesNotFoundInDatabase()
            throws BranchInjectionException, GitHubClientException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(mockGitHubRepositoryStatisticsDTO);
        when(mockGitHubRepositoryStatisticsDTO.getGitHubBranchCount(branchProtectionRegexes))
                .thenReturn(1);
        when(branchRepository.count()).thenReturn(0L);
        when(gitHubClient.getBranches()).thenReturn(Set.of(mockBranchDTO));
        when(branchMapper.toEntity(mockBranchDTO, Branch.class)).thenReturn(mockBranch);

        branchService.injectBranches();

        verify(branchRepository, times(1)).saveAll(anySet());
    }

    @Test
    public void shouldNotInjectBranches_whenBranchesAlreadyExistInDatabase()
            throws BranchInjectionException, GitHubClientException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(mockGitHubRepositoryStatisticsDTO);
        when(mockGitHubRepositoryStatisticsDTO.getGitHubBranchCount(branchProtectionRegexes))
                .thenReturn(10);
        when(branchRepository.count()).thenReturn(10L);

        branchService.injectBranches();

        verify(gitHubClient, never()).getBranches();
        verify(branchRepository, never()).saveAll(anySet());
    }

    @Test
    public void shouldThrowBranchInjectionException_whenGitHubClientThrowsException() throws GitHubClientException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(mockGitHubRepositoryStatisticsDTO);
        when(mockGitHubRepositoryStatisticsDTO.getGitHubBranchCount(branchProtectionRegexes))
                .thenReturn(1);
        when(branchRepository.count()).thenReturn(0L);
        when(gitHubClient.getBranches()).thenThrow(GitHubClientException.class);

        assertThrows(BranchInjectionException.class, () -> branchService.injectBranches());
    }

    @Test
    public void shouldInjectBranches_whenBranchNamesMatchRegexes()
            throws BranchInjectionException, GitHubClientException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(mockGitHubRepositoryStatisticsDTO);
        when(mockGitHubRepositoryStatisticsDTO.getGitHubBranchCount(branchProtectionRegexes))
                .thenReturn(1);
        when(branchRepository.count()).thenReturn(0L);
        when(gitHubClient.getBranches()).thenReturn(Set.of(mockBranchDTO));
        when(branchMapper.toEntity(mockBranchDTO, Branch.class)).thenReturn(mockBranch);

        branchService.injectBranches();

        ArgumentCaptor<Set<Branch>> captor = ArgumentCaptor.forClass(Set.class);
        verify(branchRepository, times(1)).saveAll(captor.capture());

        assertFalse(captor.getValue().isEmpty());
    }

    @Test
    public void shouldNotInjectBranches_whenBranchNamesDoNotMatchRegexes()
            throws BranchInjectionException, GitHubClientException {
        BranchDTO newBranchDTO = new BranchDTO();
        newBranchDTO.setName("feature/branch");

        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(mockGitHubRepositoryStatisticsDTO);
        when(mockGitHubRepositoryStatisticsDTO.getGitHubBranchCount(branchProtectionRegexes))
                .thenReturn(1);
        when(branchRepository.count()).thenReturn(0L);
        when(gitHubClient.getBranches()).thenReturn(Set.of(newBranchDTO));

        branchService.injectBranches();

        ArgumentCaptor<Set<Branch>> captor = ArgumentCaptor.forClass(Set.class);
        verify(branchRepository, times(1)).saveAll(captor.capture());

        assertTrue(captor.getValue().isEmpty());
    }

    @Test
    public void shouldCheckIfBranchContainsCommit_whenBranchContainsCommit() {
        boolean containsCommit = branchService.doesBranchContainCommit(
                mockBranch.getName(), Set.of(mockBranchCommit), mockCommit.getSha());

        assertTrue(containsCommit);
    }

    @Test
    public void shouldCheckIfBranchContainsCommit_whenBranchDoesNotContainCommit() {
        Commit newCommit = new Commit();
        newCommit.setSha("sha456");

        boolean containsCommit = branchService.doesBranchContainCommit(
                mockBranch.getName(), Set.of(mockBranchCommit), newCommit.getSha());

        assertFalse(containsCommit);
    }

    @Test
    public void shouldGetBranchCommitsByBranchId() {
        Set<BranchCommit> branchCommits = Set.of(mockBranchCommit);

        when(branchCommitRepository.findAllByBranch_Id(mockBranch.getId())).thenReturn(branchCommits);

        Set<BranchCommit> result = branchService.getBranchCommitsByBranchId(mockBranch.getId());

        assertEquals(1, result.size());
        assertTrue(result.contains(mockBranchCommit));
    }

    @Test
    public void shouldGetBranchPullRequestsByBranchId() {
        Set<BranchPullRequest> branchPullRequests = Set.of(mockBranchPullRequest);

        when(branchPullRequestRepository.findAllByBranch_Id(mockBranch.getId())).thenReturn(branchPullRequests);

        Set<BranchPullRequest> result = branchService.getBranchPullRequestsByBranchId(mockBranch.getId());

        assertEquals(1, result.size());
        assertTrue(result.contains(mockBranchPullRequest));
    }

    @Test
    public void shouldSaveBranches() {
        branchService.saveBranches(Set.of(mockBranch));

        verify(branchRepository, times(1)).saveAll(anySet());
    }

    @Test
    public void shouldGetAllBranches() {
        List<Branch> branches = List.of(mockBranch);
        when(branchRepository.findAll()).thenReturn(branches);

        List<Branch> result = branchService.getAllBranches();

        assertEquals(branches, result);
    }
}
