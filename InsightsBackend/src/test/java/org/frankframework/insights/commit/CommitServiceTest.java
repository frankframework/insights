package org.frankframework.insights.commit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.common.configuration.GitHubProperties;
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
    private GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private Mapper commitMapper;

    @Mock
    private CommitRepository commitRepository;

    @Mock
    private BranchService branchService;

    @InjectMocks
    private CommitService commitService;

    private Branch mockBranch;
    private CommitDTO mockCommit;
    private GitHubRepositoryStatisticsDTO mockGitHubRepositoryStatisticsDTO;

    @BeforeEach
    void setUp() {
        mockBranch = new Branch();
        mockBranch.setName("master");

        mockCommit = new CommitDTO();
        mockCommit.oid = "sha123";

        mockGitHubRepositoryStatisticsDTO = mock(GitHubRepositoryStatisticsDTO.class);
        when(gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO())
                .thenReturn(mockGitHubRepositoryStatisticsDTO);
    }

    @Test
    public void should_InjectCommitsForBranch_when_DatabaseIsNotFilledYet()
            throws GitHubClientException, CommitInjectionException, MappingException {
        when(mockGitHubRepositoryStatisticsDTO.getGitHubCommitCount(eq(Collections.emptyList())))
                .thenReturn(10);
        when(commitRepository.countAllBranchCommitRelations()).thenReturn(0);
        when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));
        when(gitHubClient.getBranchCommits(mockBranch.getName())).thenReturn(Set.of(mockCommit));
        when(commitMapper.toEntity(any(), any())).thenReturn(Set.of(mockCommit));

        commitService.injectBranchCommits();

        verify(branchService, times(1)).saveBranches(anySet());
    }

    @Test
    public void should_NotInjectCommitsForBranch_when_DatabaseIsAlreadyFilled() throws CommitInjectionException {
        when(mockGitHubRepositoryStatisticsDTO.getGitHubCommitCount(eq(Collections.emptyList())))
                .thenReturn(10);
        when(commitRepository.countAllBranchCommitRelations()).thenReturn(10);

        commitService.injectBranchCommits();

        verify(branchService, never()).saveBranches(anySet());
    }

    @Test
    public void should_ThrowCommitInjectionException_when_ErrorOccurs() throws GitHubClientException {
        when(mockGitHubRepositoryStatisticsDTO.getGitHubCommitCount(eq(Collections.emptyList())))
                .thenReturn(10);
        when(commitRepository.countAllBranchCommitRelations()).thenReturn(0);
        when(branchService.getAllBranches()).thenReturn(List.of(mockBranch));
        when(gitHubClient.getBranchCommits(mockBranch.getName()))
                .thenThrow(new GitHubClientException("Error fetching commits", null));

        assertThrows(CommitInjectionException.class, () -> commitService.injectBranchCommits());
    }
}
