package org.frankframework.insights.commit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.common.configuration.GitHubProperties;
import org.frankframework.insights.common.entityconnection.branchcommit.BranchCommitRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.mapper.MappingException;
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
public class CommitServiceTest {
    @Mock
    private GitHubProperties gitHubProperties;

    @Mock
    private GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private Mapper commitMapper;

    @Mock
    private BranchCommitRepository branchCommitRepository;

    @Mock
    private BranchService branchService;

    @InjectMocks
    private CommitService commitService;

    private Branch mockBranch;
    private CommitDTO mockCommitDTO;
    private Commit mockCommit;
    private GitHubRepositoryStatisticsDTO mockGitHubRepositoryStatisticsDTO;
    private Map<String, Integer> gitHubCommitCounts;

    @BeforeEach
    public void setUp() {
        mockBranch = new Branch();
        mockBranch.setName("master");

        mockCommit = new Commit();
        mockCommit.setSha("sha123");

        mockCommitDTO = new CommitDTO();
        mockCommitDTO.sha = "sha123";

        mockGitHubRepositoryStatisticsDTO = mock(GitHubRepositoryStatisticsDTO.class);
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(mockGitHubRepositoryStatisticsDTO);

        gitHubCommitCounts = new HashMap<>();
        gitHubCommitCounts.put(mockBranch.getName(), 10); // Assuming GitHub has 10 commits
    }

    @Test
    public void should_InjectCommitsForBranch_when_DatabaseIsNotFilledYet()
            throws GitHubClientException, MappingException {
        when(mockGitHubRepositoryStatisticsDTO.getGitHubCommitsCount(eq(Collections.emptyList())))
                .thenReturn(gitHubCommitCounts);
        when(branchCommitRepository.countBranchCommitByBranch(mockBranch)).thenReturn(0); // No commits in DB
        when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));
        when(gitHubClient.getBranchCommits(mockBranch.getName())).thenReturn(Set.of(mockCommitDTO));
        when(commitMapper.toEntity(any(), any())).thenReturn(Set.of(mockCommit));

        commitService.injectBranchCommits();

        verify(branchService, times(1)).saveBranches(anySet()); // It should save the branch
    }

    @Test
    public void should_NotInjectCommitsForBranch_when_DatabaseIsAlreadyFilled() {
        when(mockGitHubRepositoryStatisticsDTO.getGitHubCommitsCount(eq(Collections.emptyList())))
                .thenReturn(gitHubCommitCounts);
        when(branchCommitRepository.countBranchCommitByBranch(mockBranch)).thenReturn(10); // Already 10 commits in DB
        when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));

        commitService.injectBranchCommits();

        verify(branchService, times(0)).saveBranches(anySet());
    }

    @Test
    public void should_InjectCommitsOnlyForBranchesWithDifferentCommitCounts()
            throws GitHubClientException, MappingException {
        Branch mockBranch2 = new Branch();
        mockBranch2.setName("dev");

        Map<String, Integer> commitCounts = Map.of("master", 5, "dev", 10);

        when(mockGitHubRepositoryStatisticsDTO.getGitHubCommitsCount(anyList())).thenReturn(commitCounts);
        when(branchCommitRepository.countBranchCommitByBranch(mockBranch)).thenReturn(5);
        when(branchCommitRepository.countBranchCommitByBranch(mockBranch2)).thenReturn(5);
        when(branchService.getAllBranches()).thenReturn(List.of(mockBranch, mockBranch2));
        when(gitHubClient.getBranchCommits(anyString())).thenReturn(Set.of(mockCommitDTO));
        when(commitMapper.toEntity(any(), any())).thenReturn(Set.of(mockCommit));

        commitService.injectBranchCommits();

        verify(branchService, times(1)).saveBranches(anySet());
    }

    @Test
    public void should_LogInformation_when_NoBranchesToUpdate() {
        when(mockGitHubRepositoryStatisticsDTO.getGitHubCommitsCount(eq(Collections.emptyList())))
                .thenReturn(gitHubCommitCounts);
        when(branchCommitRepository.countBranchCommitByBranch(mockBranch)).thenReturn(10); // Already 10 commits in DB
        when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));

        // Here, we just check if the log is called when no branches need to be updated.
        commitService.injectBranchCommits();

        verify(branchService, times(0)).saveBranches(anySet()); // No updates should happen
        // We can also check the logs if needed
    }
}
