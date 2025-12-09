package org.frankframework.insights.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequest;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequestRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.properties.GitHubProperties;
import org.frankframework.insights.github.graphql.GitHubGraphQLClient;
import org.frankframework.insights.github.graphql.GitHubGraphQLClientException;
import org.frankframework.insights.github.graphql.GitHubRepositoryStatisticsDTO;
import org.frankframework.insights.github.graphql.GitHubRepositoryStatisticsService;
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
    private GitHubGraphQLClient gitHubGraphQLClient;

    @Mock
    private Mapper mapper;

    @Mock
    private BranchRepository branchRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
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
    public void setUp() {
        statsDTO = mock(GitHubRepositoryStatisticsDTO.class);

        branchProtectionRegexes = List.of("^master$", "^release/\\d+\\.\\d+$", "^\\d+\\.\\d+-release$");
        when(gitHubProperties.getGraphql().getBranchProtectionRegexes()).thenReturn(branchProtectionRegexes);

        protectedBranchDTO = new BranchDTO("id1", "release/1.2");
        protectedBranch = new Branch();
        protectedBranch.setId(UUID.randomUUID().toString());
        protectedBranch.setName("release/1.2");

        unprotectedBranchDTO = new BranchDTO("id2", "feature/test");

        mockPullRequest = new PullRequest();
        mockPullRequest.setId(UUID.randomUUID().toString());

        branchService = new BranchService(
                gitHubRepositoryStatisticsService,
                gitHubGraphQLClient,
                mapper,
                branchRepository,
                gitHubProperties,
                branchPullRequestRepository);
    }

    @Test
    public void injectBranches_shouldSkip_whenCountsEqual() throws GitHubGraphQLClientException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statsDTO);
        when(statsDTO.getGitHubBranchCount(branchProtectionRegexes)).thenReturn(4);
        when(branchRepository.count()).thenReturn(4L);

        assertDoesNotThrow(() -> branchService.injectBranches());
        verify(gitHubGraphQLClient, never()).getBranches();
        verify(branchRepository, never()).saveAll(anySet());
    }

    @Test
    public void injectBranches_shouldInject_andFilterProtectedBranches()
            throws GitHubGraphQLClientException, BranchInjectionException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statsDTO);
        when(statsDTO.getGitHubBranchCount(branchProtectionRegexes)).thenReturn(2);
        when(branchRepository.count()).thenReturn(0L);
        Set<BranchDTO> branchDTOs = Set.of(protectedBranchDTO, unprotectedBranchDTO);
        when(gitHubGraphQLClient.getBranches()).thenReturn(branchDTOs);
        when(mapper.toEntity(protectedBranchDTO, Branch.class)).thenReturn(protectedBranch);
        when(branchRepository.findAll()).thenReturn(Collections.emptyList());

        branchService.injectBranches();

        ArgumentCaptor<Set<Branch>> captor = ArgumentCaptor.forClass(Set.class);
        verify(branchRepository).saveAll(captor.capture());
        Set<Branch> saved = captor.getValue();
        assertEquals(1, saved.size());
        assertEquals("release/1.2", saved.iterator().next().getName());
    }

    @Test
    public void injectBranches_shouldHandleEmptyOrNullBranchDTOSet()
            throws GitHubGraphQLClientException, BranchInjectionException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statsDTO);
        when(statsDTO.getGitHubBranchCount(branchProtectionRegexes)).thenReturn(1);
        when(branchRepository.count()).thenReturn(0L);
        when(gitHubGraphQLClient.getBranches()).thenReturn(Collections.emptySet());

        branchService.injectBranches();

        verify(branchRepository, never()).saveAll(anySet());
    }

    @Test
    public void injectBranches_shouldThrowBranchInjectionException_onGitHubFailure()
            throws GitHubGraphQLClientException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statsDTO);
        when(statsDTO.getGitHubBranchCount(branchProtectionRegexes)).thenReturn(1);
        when(branchRepository.count()).thenReturn(0L);
        when(gitHubGraphQLClient.getBranches()).thenThrow(new RuntimeException("fail"));

        assertThrows(BranchInjectionException.class, () -> branchService.injectBranches());
    }

    @Test
    public void injectBranches_shouldThrowBranchInjectionException_onMapperFailure()
            throws GitHubGraphQLClientException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statsDTO);
        when(statsDTO.getGitHubBranchCount(branchProtectionRegexes)).thenReturn(1);
        when(branchRepository.count()).thenReturn(0L);
        Set<BranchDTO> branchDTOs = Set.of(protectedBranchDTO);
        when(gitHubGraphQLClient.getBranches()).thenReturn(branchDTOs);
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

    @Test
    public void injectBranches_shouldExcludeRenovateBranches()
            throws GitHubGraphQLClientException, BranchInjectionException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statsDTO);
        when(statsDTO.getGitHubBranchCount(branchProtectionRegexes)).thenReturn(1);
        when(branchRepository.count()).thenReturn(0L);

        BranchDTO renovateBranchDTO = new BranchDTO("id3", "renovate/release/9.0-docker-(lts)");
        BranchDTO renovateMasterBranchDTO = new BranchDTO("id4", "renovate/master-aspose");
        BranchDTO invalidBranchDTO = new BranchDTO("id5", "release/9.0-docker");
        BranchDTO validReleaseFormatBranchDTO = new BranchDTO("id6", "9.0-release");
        Branch validReleaseFormatBranch = new Branch();
        validReleaseFormatBranch.setId("id6");
        validReleaseFormatBranch.setName("9.0-release");

        Set<BranchDTO> branchDTOs = Set.of(
                protectedBranchDTO,
                renovateBranchDTO,
                renovateMasterBranchDTO,
                invalidBranchDTO,
                validReleaseFormatBranchDTO);
        when(gitHubGraphQLClient.getBranches()).thenReturn(branchDTOs);
        when(mapper.toEntity(protectedBranchDTO, Branch.class)).thenReturn(protectedBranch);
        when(mapper.toEntity(validReleaseFormatBranchDTO, Branch.class)).thenReturn(validReleaseFormatBranch);
        when(branchRepository.findAll()).thenReturn(Collections.emptyList());

        branchService.injectBranches();

        ArgumentCaptor<Set<Branch>> captor = ArgumentCaptor.forClass(Set.class);
        verify(branchRepository).saveAll(captor.capture());
        Set<Branch> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertTrue(saved.stream().anyMatch(b -> b.getName().equals("release/1.2")));
        assertTrue(saved.stream().anyMatch(b -> b.getName().equals("9.0-release")));
        assertFalse(saved.stream().anyMatch(b -> b.getName().startsWith("renovate/")));
        assertFalse(saved.stream().anyMatch(b -> b.getName().equals("release/9.0-docker")));
    }

    @Test
    public void injectBranches_shouldCleanupOrphanedBranches()
            throws GitHubGraphQLClientException, BranchInjectionException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statsDTO);
        when(statsDTO.getGitHubBranchCount(branchProtectionRegexes)).thenReturn(1);
        when(branchRepository.count()).thenReturn(2L);

        Branch orphanedBranch = new Branch();
        orphanedBranch.setId("orphan-id");
        orphanedBranch.setName("old-branch");

        Set<BranchDTO> branchDTOs = Set.of(protectedBranchDTO);
        when(gitHubGraphQLClient.getBranches()).thenReturn(branchDTOs);
        when(mapper.toEntity(protectedBranchDTO, Branch.class)).thenReturn(protectedBranch);
        when(branchRepository.findAll()).thenReturn(List.of(protectedBranch, orphanedBranch));

        branchService.injectBranches();

        ArgumentCaptor<List<Branch>> deleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(branchRepository).deleteAll(deleteCaptor.capture());
        List<Branch> deleted = deleteCaptor.getValue();
        assertEquals(1, deleted.size());
        assertEquals("orphan-id", deleted.getFirst().getId());
    }

    @Test
    public void injectBranches_shouldNotDeleteBranches_whenNoOrphans()
            throws GitHubGraphQLClientException, BranchInjectionException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statsDTO);
        when(statsDTO.getGitHubBranchCount(branchProtectionRegexes)).thenReturn(1);
        when(branchRepository.count()).thenReturn(0L);

        Set<BranchDTO> branchDTOs = Set.of(protectedBranchDTO);
        when(gitHubGraphQLClient.getBranches()).thenReturn(branchDTOs);
        when(mapper.toEntity(protectedBranchDTO, Branch.class)).thenReturn(protectedBranch);
        when(branchRepository.findAll()).thenReturn(Collections.emptyList());

        branchService.injectBranches();

        verify(branchRepository, never()).deleteAll(anyList());
    }

    @Test
    public void injectBranches_shouldOnlyMatchStrictBranchPatterns()
            throws GitHubGraphQLClientException, BranchInjectionException {
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(statsDTO);
        when(statsDTO.getGitHubBranchCount(branchProtectionRegexes)).thenReturn(1);
        when(branchRepository.count()).thenReturn(0L);

        BranchDTO masterBranchDTO = new BranchDTO("master-id", "master");
        BranchDTO release12DTO = new BranchDTO("release12-id", "release/1.2");
        BranchDTO release90DTO = new BranchDTO("release90-id", "release/9.0");
        BranchDTO release100DTO = new BranchDTO("release100-id", "release/10.0");
        BranchDTO version12DTO = new BranchDTO("version12-id", "1.2-release");
        BranchDTO version90DTO = new BranchDTO("version90-id", "9.0-release");
        BranchDTO version100DTO = new BranchDTO("version100-id", "10.0-release");

        BranchDTO masterDevDTO = new BranchDTO("master-dev-id", "master-dev");
        BranchDTO releaseInvalidDTO = new BranchDTO("release-invalid-id", "release/9.0-docker");
        BranchDTO releaseAlphaDTO = new BranchDTO("release-alpha-id", "release/v1.2");
        BranchDTO renovateDTO = new BranchDTO("renovate-id", "renovate/release/9.0");
        BranchDTO featureDTO = new BranchDTO("feature-id", "feature/test");

        Branch masterBranch = new Branch();
        masterBranch.setId("master-id");
        masterBranch.setName("master");

        Branch release12Branch = new Branch();
        release12Branch.setId("release12-id");
        release12Branch.setName("release/1.2");

        Branch release90Branch = new Branch();
        release90Branch.setId("release90-id");
        release90Branch.setName("release/9.0");

        Branch release100Branch = new Branch();
        release100Branch.setId("release100-id");
        release100Branch.setName("release/10.0");

        Branch version12Branch = new Branch();
        version12Branch.setId("version12-id");
        version12Branch.setName("1.2-release");

        Branch version90Branch = new Branch();
        version90Branch.setId("version90-id");
        version90Branch.setName("9.0-release");

        Branch version100Branch = new Branch();
        version100Branch.setId("version100-id");
        version100Branch.setName("10.0-release");

        Set<BranchDTO> branchDTOs = Set.of(
                masterBranchDTO,
                release12DTO,
                release90DTO,
                release100DTO,
                version12DTO,
                version90DTO,
                version100DTO,
                masterDevDTO,
                releaseInvalidDTO,
                releaseAlphaDTO,
                renovateDTO,
                featureDTO);

        when(gitHubGraphQLClient.getBranches()).thenReturn(branchDTOs);
        when(mapper.toEntity(masterBranchDTO, Branch.class)).thenReturn(masterBranch);
        when(mapper.toEntity(release12DTO, Branch.class)).thenReturn(release12Branch);
        when(mapper.toEntity(release90DTO, Branch.class)).thenReturn(release90Branch);
        when(mapper.toEntity(release100DTO, Branch.class)).thenReturn(release100Branch);
        when(mapper.toEntity(version12DTO, Branch.class)).thenReturn(version12Branch);
        when(mapper.toEntity(version90DTO, Branch.class)).thenReturn(version90Branch);
        when(mapper.toEntity(version100DTO, Branch.class)).thenReturn(version100Branch);
        when(branchRepository.findAll()).thenReturn(Collections.emptyList());

        branchService.injectBranches();

        ArgumentCaptor<Set<Branch>> captor = ArgumentCaptor.forClass(Set.class);
        verify(branchRepository).saveAll(captor.capture());
        Set<Branch> saved = captor.getValue();

        assertEquals(7, saved.size());

        assertTrue(saved.stream().anyMatch(b -> b.getName().equals("master")));
        assertTrue(saved.stream().anyMatch(b -> b.getName().equals("release/1.2")));
        assertTrue(saved.stream().anyMatch(b -> b.getName().equals("release/9.0")));
        assertTrue(saved.stream().anyMatch(b -> b.getName().equals("release/10.0")));
        assertTrue(saved.stream().anyMatch(b -> b.getName().equals("1.2-release")));
        assertTrue(saved.stream().anyMatch(b -> b.getName().equals("9.0-release")));
        assertTrue(saved.stream().anyMatch(b -> b.getName().equals("10.0-release")));

        assertFalse(saved.stream().anyMatch(b -> b.getName().equals("master-dev")));
        assertFalse(saved.stream().anyMatch(b -> b.getName().equals("release/9.0-docker")));
        assertFalse(saved.stream().anyMatch(b -> b.getName().equals("release/v1.2")));
        assertFalse(saved.stream().anyMatch(b -> b.getName().startsWith("renovate/")));
        assertFalse(saved.stream().anyMatch(b -> b.getName().equals("feature/test")));
    }
}
