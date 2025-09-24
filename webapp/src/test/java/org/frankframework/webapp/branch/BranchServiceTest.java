package org.frankframework.webapp.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.frankframework.shared.entity.Branch;
import org.frankframework.webapp.common.configuration.properties.GitHubProperties;
import org.frankframework.webapp.common.entityconnection.branchpullrequest.BranchPullRequest;
import org.frankframework.webapp.common.entityconnection.branchpullrequest.BranchPullRequestRepository;
import org.frankframework.webapp.common.mapper.Mapper;
import org.frankframework.webapp.github.GitHubClient;
import org.frankframework.webapp.github.GitHubClientException;
import org.frankframework.webapp.github.GitHubRepositoryStatisticsDTO;
import org.frankframework.webapp.github.GitHubRepositoryStatisticsService;
import org.frankframework.webapp.pullrequest.PullRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BranchServiceTest {

    @Mock
    private GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private Mapper mapper;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private GitHubProperties gitHubProperties;

    @Mock
    private BranchPullRequestRepository branchPullRequestRepository;

    @InjectMocks
    private BranchService branchService;

    private GitHubRepositoryStatisticsDTO statsDTO;
    private List<String> branchProtectionRegexes;
    private BranchDTO protectedBranchDTO;
    private BranchDTO unprotectedBranchDTO;
    private Branch protectedBranch;
    private PullRequest mockPullRequest;

    @BeforeEach
    void setUp() {
        statsDTO = mock(GitHubRepositoryStatisticsDTO.class);

        branchProtectionRegexes = List.of("release/.*", "main", "master");
        when(gitHubProperties.getBranchProtectionRegexes()).thenReturn(branchProtectionRegexes);

        protectedBranchDTO = new BranchDTO("id1", "release/v1.2.3");
        protectedBranch = new Branch();
        protectedBranch.setId(UUID.randomUUID().toString());
        protectedBranch.setName("release/v1.2.3");

        unprotectedBranchDTO = new BranchDTO("id2", "feature/test");

        mockPullRequest = new PullRequest();
        mockPullRequest.setId(UUID.randomUUID().toString());

        branchService = new BranchService(
                gitHubRepositoryStatisticsService,
                gitHubClient,
                mapper,
                branchRepository,
                gitHubProperties,
                branchPullRequestRepository);
    }

    @Test
    public void injectBranches_shouldSkip_whenCountsEqual() throws GitHubClientException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statsDTO);
        when(statsDTO.getGitHubBranchCount(branchProtectionRegexes)).thenReturn(4);
        when(branchRepository.count()).thenReturn(4L);

        assertDoesNotThrow(() -> branchService.injectBranches());
        verify(gitHubClient, never()).getBranches();
        verify(branchRepository, never()).saveAll(anySet());
    }

    @Test
    public void injectBranches_shouldInject_andFilterProtectedBranches()
            throws GitHubClientException, BranchInjectionException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statsDTO);
        when(statsDTO.getGitHubBranchCount(branchProtectionRegexes)).thenReturn(2);
        when(branchRepository.count()).thenReturn(0L);
        Set<BranchDTO> branchDTOs = Set.of(protectedBranchDTO, unprotectedBranchDTO);
        when(gitHubClient.getBranches()).thenReturn(branchDTOs);
        when(mapper.toEntity(protectedBranchDTO, Branch.class)).thenReturn(protectedBranch);

        branchService.injectBranches();

        ArgumentCaptor<Set<Branch>> captor = ArgumentCaptor.forClass(Set.class);
        verify(branchRepository).saveAll(captor.capture());
        Set<Branch> saved = captor.getValue();
        assertEquals(1, saved.size());
        assertTrue(saved.iterator().next().getName().startsWith("release/"));
    }

    @Test
    public void injectBranches_shouldHandleEmptyOrNullBranchDTOSet()
            throws GitHubClientException, BranchInjectionException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statsDTO);
        when(statsDTO.getGitHubBranchCount(branchProtectionRegexes)).thenReturn(1);
        when(branchRepository.count()).thenReturn(0L);
        when(gitHubClient.getBranches()).thenReturn(Collections.emptySet());

        branchService.injectBranches();

        verify(branchRepository, never()).saveAll(anySet());
    }

    @Test
    public void injectBranches_shouldThrowBranchInjectionException_onGitHubFailure() throws GitHubClientException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statsDTO);
        when(statsDTO.getGitHubBranchCount(branchProtectionRegexes)).thenReturn(1);
        when(branchRepository.count()).thenReturn(0L);
        when(gitHubClient.getBranches()).thenThrow(new RuntimeException("fail"));

        assertThrows(BranchInjectionException.class, () -> branchService.injectBranches());
    }

    @Test
    public void injectBranches_shouldThrowBranchInjectionException_onMapperFailure() throws GitHubClientException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statsDTO);
        when(statsDTO.getGitHubBranchCount(branchProtectionRegexes)).thenReturn(1);
        when(branchRepository.count()).thenReturn(0L);
        Set<BranchDTO> branchDTOs = Set.of(protectedBranchDTO);
        when(gitHubClient.getBranches()).thenReturn(branchDTOs);
        when(mapper.toEntity(protectedBranchDTO, Branch.class)).thenThrow(new RuntimeException("mapper error"));

        assertThrows(BranchInjectionException.class, () -> branchService.injectBranches());
    }

    @Test
    public void getAllBranches_shouldReturnEmptyList_whenNoBranches() {
        when(branchRepository.findAll()).thenReturn(Collections.emptyList());
        List<Branch> result = branchService.getAllBranches();
        assertTrue(result.isEmpty());
    }

    @Test
    public void getAllBranches_shouldReturnListOfBranches() {
        when(branchRepository.findAll()).thenReturn(List.of(protectedBranch));
        List<Branch> result = branchService.getAllBranches();
        assertEquals(1, result.size());
        assertEquals(protectedBranch, result.getFirst());
    }

    @Test
    public void getBranchPullRequestsByBranches_shouldReturnMapForGivenBranches() {
        Set<BranchPullRequest> bprs = Set.of(new BranchPullRequest(protectedBranch, mockPullRequest));
        when(branchPullRequestRepository.findAllByBranch_Id(protectedBranch.getId()))
                .thenReturn(bprs);

        Map<String, Set<BranchPullRequest>> result =
                branchService.getBranchPullRequestsByBranches(List.of(protectedBranch));
        assertEquals(1, result.size());
        assertEquals(bprs, result.get(protectedBranch.getId()));
    }

    @Test
    public void getBranchPullRequestsByBranches_shouldReturnEmptyMapForEmptyBranchesList() {
        Map<String, Set<BranchPullRequest>> result =
                branchService.getBranchPullRequestsByBranches(Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void getBranchPullRequestsByBranchId_shouldReturnSet() {
        Set<BranchPullRequest> bprs = Set.of(new BranchPullRequest(protectedBranch, mockPullRequest));
        when(branchPullRequestRepository.findAllByBranch_Id(protectedBranch.getId()))
                .thenReturn(bprs);
        Set<BranchPullRequest> result = branchService.getBranchPullRequestsByBranchId(protectedBranch.getId());
        assertEquals(bprs, result);
    }

    @Test
    public void getBranchPullRequestsByBranchId_shouldReturnEmptySetIfNone() {
        when(branchPullRequestRepository.findAllByBranch_Id(protectedBranch.getId()))
                .thenReturn(Collections.emptySet());
        Set<BranchPullRequest> result = branchService.getBranchPullRequestsByBranchId(protectedBranch.getId());
        assertTrue(result.isEmpty());
    }

    @Test
    public void saveBranches_shouldSaveBranches() {
        Set<Branch> branches = Set.of(protectedBranch);
        when(branchRepository.saveAll(branches)).thenReturn(List.of(protectedBranch));
        assertDoesNotThrow(() -> branchService.saveBranches(branches));
        verify(branchRepository).saveAll(branches);
    }

    @Test
    public void saveBranches_shouldLogAndNotThrow_whenEmptyInput() {
        Set<Branch> branches = Collections.emptySet();
        when(branchRepository.saveAll(branches)).thenReturn(Collections.emptyList());
        assertDoesNotThrow(() -> branchService.saveBranches(branches));
        verify(branchRepository).saveAll(branches);
    }
}
