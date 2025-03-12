package org.frankframework.insights.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.configuration.GitHubProperties;
import org.frankframework.insights.dto.BranchDTO;
import org.frankframework.insights.exceptions.branches.BranchDatabaseException;
import org.frankframework.insights.exceptions.branches.BranchInjectionException;
import org.frankframework.insights.exceptions.clients.GitHubClientException;
import org.frankframework.insights.mapper.Mapper;
import org.frankframework.insights.models.Branch;
import org.frankframework.insights.models.Commit;
import org.frankframework.insights.repository.BranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

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

    @BeforeEach
    void setUp() {
        when(gitHubProperties.getProtectionRegex()).thenReturn("release");

        mockBranchDTO = new BranchDTO();
        mockBranchDTO.setName("release/v1.0.0");

        mockBranch = new Branch();
        mockBranch.setName("release/v1.0.0");

        mockCommit = new Commit();
        mockCommit.setOid("sha123");

        mockBranch.setCommits(Set.of(mockCommit));

        branchService = new BranchService(gitHubClient, branchMapper, branchRepository, gitHubProperties);
    }

    @Test
    public void should_InjectBranches_when_BranchesNotFoundInDatabase()
            throws BranchInjectionException, GitHubClientException {
        // Arrange: No branches in the database
        when(branchRepository.findAll()).thenReturn(Collections.emptyList());
        when(gitHubClient.getBranches()).thenReturn(Set.of(mockBranchDTO));
        when(branchMapper.toEntity(mockBranchDTO, Branch.class)).thenReturn(mockBranch);

        branchService.injectBranches();

        verify(branchRepository, times(1)).saveAll(anySet());
    }

    @Test
    public void should_NotInjectBranches_when_BranchesAlreadyExistInDatabase()
            throws BranchInjectionException, GitHubClientException {
        when(branchRepository.findAll()).thenReturn(List.of(mockBranch));

        branchService.injectBranches();

        verify(gitHubClient, times(0)).getBranches();
        verify(branchRepository, times(0)).saveAll(anySet());
    }

    @Test
    public void should_ThrowBranchInjectionException_when_GitHubClientThrowsException() throws GitHubClientException {
        when(branchRepository.findAll()).thenReturn(Collections.emptyList());
        when(gitHubClient.getBranches()).thenThrow(new GitHubClientException("GitHub client branch error", null));

        assertThrows(BranchInjectionException.class, () -> branchService.injectBranches());
    }

    @Test
    public void should_CheckIfBranchContainsCommit_when_BranchContainsCommit() throws BranchDatabaseException {
        mockBranch.setCommits(Set.of(mockCommit));

        boolean containsCommit = branchService.doesBranchContainCommit(mockBranch, mockCommit.getOid());

        assertTrue(containsCommit);
    }
}
