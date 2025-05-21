// package org.frankframework.insights.branch;
//
// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.*;
//
// import java.util.*;
// import org.frankframework.insights.common.configuration.properties.GitHubProperties;
// import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequest;
// import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequestRepository;
// import org.frankframework.insights.common.mapper.Mapper;
// import org.frankframework.insights.github.GitHubClient;
// import org.frankframework.insights.github.GitHubClientException;
// import org.frankframework.insights.github.GitHubRepositoryStatisticsDTO;
// import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
// import org.frankframework.insights.pullrequest.PullRequest;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.ArgumentCaptor;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
//
// @ExtendWith(MockitoExtension.class)
// public class BranchServiceTest {
//
//    @Mock
//    private GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;
//
//    @Mock
//    private GitHubClient gitHubClient;
//
//    @Mock
//    private Mapper branchMapper;
//
//    @Mock
//    private BranchRepository branchRepository;
//
//    @Mock
//    private BranchPullRequestRepository branchPullRequestRepository;
//
//    @Mock
//    private GitHubProperties gitHubProperties;
//
//    @InjectMocks
//    private BranchService branchService;
//
//    private BranchDTO mockBranchDTO;
//    private Branch mockBranch;
//    private BranchPullRequest mockBranchPullRequest;
//    private GitHubRepositoryStatisticsDTO mockGitHubRepositoryStatisticsDTO;
//
//    private List<String> branchProtectionRegexes;
//
//    @BeforeEach
//    public void setUp() {
//        mockBranchDTO = new BranchDTO();
//        mockBranchDTO.setName("release/v1.0.0");
//
//        mockBranch = new Branch();
//        mockBranch.setName("release/v1.0.0");
//
//        PullRequest mockPullRequest = new PullRequest();
//        mockPullRequest.setId(UUID.randomUUID().toString());
//
//        mockBranchPullRequest = new BranchPullRequest(mockBranch, mockPullRequest);
//
//        mockGitHubRepositoryStatisticsDTO = mock(GitHubRepositoryStatisticsDTO.class);
//
//        branchProtectionRegexes = List.of("release", "master");
//
//        when(gitHubProperties.getBranchProtectionRegexes()).thenReturn(branchProtectionRegexes);
//
//        branchService = new BranchService(
//                gitHubRepositoryStatisticsService,
//                gitHubClient,
//                branchMapper,
//                branchRepository,
//                gitHubProperties,
//                branchPullRequestRepository);
//    }
//
//    @Test
//    public void shouldInjectBranches_whenBranchesNotFoundInDatabase()
//            throws BranchInjectionException, GitHubClientException {
//        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
//                .thenReturn(mockGitHubRepositoryStatisticsDTO);
//        when(mockGitHubRepositoryStatisticsDTO.getGitHubBranchCount(branchProtectionRegexes))
//                .thenReturn(1);
//        when(branchRepository.count()).thenReturn(0L);
//        when(gitHubClient.getBranches()).thenReturn(Set.of(mockBranchDTO));
//        when(branchMapper.toEntity(mockBranchDTO, Branch.class)).thenReturn(mockBranch);
//
//        branchService.injectBranches();
//
//        verify(branchRepository, times(1)).saveAll(anySet());
//    }
//
//    @Test
//    public void shouldNotInjectBranches_whenBranchesAlreadyExistInDatabase()
//            throws BranchInjectionException, GitHubClientException {
//        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
//                .thenReturn(mockGitHubRepositoryStatisticsDTO);
//        when(mockGitHubRepositoryStatisticsDTO.getGitHubBranchCount(branchProtectionRegexes))
//                .thenReturn(10);
//        when(branchRepository.count()).thenReturn(10L);
//
//        branchService.injectBranches();
//
//        verify(gitHubClient, never()).getBranches();
//        verify(branchRepository, never()).saveAll(anySet());
//    }
//
//    @Test
//    public void shouldThrowBranchInjectionException_whenGitHubClientThrowsException() throws GitHubClientException {
//        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
//                .thenReturn(mockGitHubRepositoryStatisticsDTO);
//        when(mockGitHubRepositoryStatisticsDTO.getGitHubBranchCount(branchProtectionRegexes))
//                .thenReturn(1);
//        when(branchRepository.count()).thenReturn(0L);
//        when(gitHubClient.getBranches()).thenThrow(GitHubClientException.class);
//
//        assertThrows(BranchInjectionException.class, () -> branchService.injectBranches());
//    }
//
//    @Test
//    public void shouldInjectBranches_whenBranchNamesMatchRegexes()
//            throws BranchInjectionException, GitHubClientException {
//        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
//                .thenReturn(mockGitHubRepositoryStatisticsDTO);
//        when(mockGitHubRepositoryStatisticsDTO.getGitHubBranchCount(branchProtectionRegexes))
//                .thenReturn(1);
//        when(branchRepository.count()).thenReturn(0L);
//        when(gitHubClient.getBranches()).thenReturn(Set.of(mockBranchDTO));
//        when(branchMapper.toEntity(mockBranchDTO, Branch.class)).thenReturn(mockBranch);
//
//        branchService.injectBranches();
//
//        ArgumentCaptor<Set<Branch>> captor = ArgumentCaptor.forClass(Set.class);
//        verify(branchRepository, times(1)).saveAll(captor.capture());
//
//        assertFalse(captor.getValue().isEmpty());
//    }
//
//    @Test
//    public void shouldNotInjectBranches_whenBranchNamesDoNotMatchRegexes()
//            throws BranchInjectionException, GitHubClientException {
//        BranchDTO newBranchDTO = new BranchDTO();
//        newBranchDTO.setName("feature/branch");
//
//        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
//                .thenReturn(mockGitHubRepositoryStatisticsDTO);
//        when(mockGitHubRepositoryStatisticsDTO.getGitHubBranchCount(branchProtectionRegexes))
//                .thenReturn(1);
//        when(branchRepository.count()).thenReturn(0L);
//        when(gitHubClient.getBranches()).thenReturn(Set.of(newBranchDTO));
//
//        branchService.injectBranches();
//
//        ArgumentCaptor<Set<Branch>> captor = ArgumentCaptor.forClass(Set.class);
//        verify(branchRepository, times(1)).saveAll(captor.capture());
//
//        assertTrue(captor.getValue().isEmpty());
//    }
//
//    @Test
//    public void shouldGetBranchPullRequestsByBranchId() {
//        Set<BranchPullRequest> branchPullRequests = Set.of(mockBranchPullRequest);
//
//        when(branchPullRequestRepository.findAllByBranch_Id(mockBranch.getId())).thenReturn(branchPullRequests);
//
//        Set<BranchPullRequest> result = branchService.getBranchPullRequestsByBranchId(mockBranch.getId());
//
//        assertEquals(1, result.size());
//        assertTrue(result.contains(mockBranchPullRequest));
//    }
//
//	@Test
//    public void shouldSaveBranches() {
//        branchService.saveBranches(Set.of(mockBranch));
//
//        verify(branchRepository, times(1)).saveAll(anySet());
//    }
//
//    @Test
//    public void shouldGetAllBranches() {
//        List<Branch> branches = List.of(mockBranch);
//        when(branchRepository.findAll()).thenReturn(branches);
//
//        List<Branch> result = branchService.getAllBranches();
//
//        assertEquals(branches, result);
//    }

package org.frankframework.insights.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.frankframework.insights.common.configuration.properties.GitHubProperties;
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

        protectedBranchDTO = new BranchDTO();
        protectedBranchDTO.setName("release/v1.2.3");
        protectedBranch = new Branch();
        protectedBranch.setId(UUID.randomUUID().toString());
        protectedBranch.setName("release/v1.2.3");

        unprotectedBranchDTO = new BranchDTO();
        unprotectedBranchDTO.setName("feature/test");

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
