package org.frankframework.insights.commit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.common.configuration.GitHubProperties;
import org.frankframework.insights.common.entityconnection.branchcommit.BranchCommit;
import org.frankframework.insights.common.entityconnection.branchcommit.BranchCommitRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubRepositoryStatisticsDTO;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommitServiceTest {

    @Mock
    private GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private Mapper mapper;

    @Mock
    private BranchCommitRepository branchCommitRepository;

    @Mock
    private BranchService branchService;

    @Mock
    private GitHubProperties gitHubProperties;

    @Mock
    private CommitRepository commitRepository;

    @InjectMocks
    private CommitService commitService;

    private Branch mockBranch;
    private CommitDTO mockCommitDTO;
    private Commit mockCommit;
    private GitHubRepositoryStatisticsDTO mockStatsDTO;

    @BeforeEach
    void setUp() {
        mockBranch = new Branch();
        mockBranch.setId(UUID.randomUUID().toString());
        mockBranch.setName("master");

        mockCommitDTO = new CommitDTO();
        mockCommitDTO.sha = "sha123";

        mockCommit = new Commit();
        mockCommit.setSha("sha123");

        mockStatsDTO = mock(GitHubRepositoryStatisticsDTO.class);

        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(mockStatsDTO);
    }

    @Test
    void should_InjectNewCommits_When_DBCountDiffersFromGitHub() throws Exception {
        Map<String, Integer> githubCounts = Map.of("master", 10);

        when(mockStatsDTO.getGitHubCommitsCount(anyList())).thenReturn(githubCounts);
        when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));
        when(branchCommitRepository.countBranchCommitByBranch_Name("master")).thenReturn(0);

        when(gitHubClient.getBranchCommits("master")).thenReturn(Set.of(mockCommitDTO));
        when(mapper.toEntity(anySet(), eq(Commit.class))).thenReturn(Set.of(mockCommit));
        when(branchCommitRepository.findAllByBranch_Id(mockBranch.getId())).thenReturn(Set.of());

        commitService.injectBranchCommits();

        ArgumentCaptor<Collection<BranchCommit>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(branchCommitRepository).saveAll(captor.capture());
        Collection<BranchCommit> saved = captor.getValue();

        assertEquals(1, saved.size());
        BranchCommit bc = saved.iterator().next();
        assertEquals(mockBranch.getId(), bc.getBranch().getId());
        assertEquals("sha123", bc.getCommit().getSha());
    }

    @Test
    void should_NotInject_When_DBAndGitHubCountsMatch() {
        Map<String, Integer> githubCounts = Map.of("master", 10);

        when(mockStatsDTO.getGitHubCommitsCount(anyList())).thenReturn(githubCounts);
        when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));
        when(branchCommitRepository.countBranchCommitByBranch_Name("master")).thenReturn(10);

        commitService.injectBranchCommits();

        verify(branchCommitRepository, never()).saveAll(any());
    }

    @Test
    void should_HandleMultipleBranches_WithMixedCommitCounts() throws Exception {
        Branch mockBranch2 = new Branch();
        mockBranch2.setId(UUID.randomUUID().toString());
        mockBranch2.setName("dev");

        CommitDTO dto2 = new CommitDTO();
        dto2.sha = "sha456";

        Commit commit2 = new Commit();
        commit2.setSha("sha456");

        Map<String, Integer> githubCounts = Map.of("master", 10, "dev", 5);

        when(mockStatsDTO.getGitHubCommitsCount(anyList())).thenReturn(githubCounts);
        when(branchService.getAllBranches()).thenReturn(List.of(mockBranch, mockBranch2));

        when(branchCommitRepository.countBranchCommitByBranch_Name("master")).thenReturn(10);
        when(branchCommitRepository.countBranchCommitByBranch_Name("dev")).thenReturn(3);

        when(gitHubClient.getBranchCommits("dev")).thenReturn(Set.of(dto2));
        when(mapper.toEntity(anySet(), eq(Commit.class))).thenReturn(Set.of(commit2));
        when(branchCommitRepository.findAllByBranch_Id(mockBranch2.getId())).thenReturn(Set.of());

        commitService.injectBranchCommits();

        verify(branchCommitRepository, times(1)).saveAll(any());
    }

    @Test
    void should_SkipSaving_When_NoNewCommitsFound() throws Exception {
        when(mockStatsDTO.getGitHubCommitsCount(anyList())).thenReturn(Map.of("master", 5));
        when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));
        when(branchCommitRepository.countBranchCommitByBranch_Name("master")).thenReturn(3);
        when(gitHubClient.getBranchCommits("master")).thenReturn(Set.of(mockCommitDTO));
        when(mapper.toEntity(anySet(), eq(Commit.class))).thenReturn(Set.of(mockCommit));

        BranchCommit existing = new BranchCommit(mockBranch, mockCommit);
        when(branchCommitRepository.findAllByBranch_Id(mockBranch.getId())).thenReturn(Set.of(existing));

        commitService.injectBranchCommits();

        verify(branchCommitRepository, never()).saveAll(any());
    }

    @Test
    void should_HandleException_When_GettingCommitsFails() throws Exception {
        when(mockStatsDTO.getGitHubCommitsCount(anyList())).thenReturn(Map.of("master", 7));
        when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));
        when(branchCommitRepository.countBranchCommitByBranch_Name("master")).thenReturn(3);
        when(gitHubClient.getBranchCommits("master")).thenThrow(new RuntimeException("GitHub error"));

        assertDoesNotThrow(() -> commitService.injectBranchCommits());
        verify(branchCommitRepository, never()).saveAll(any());
    }
}
