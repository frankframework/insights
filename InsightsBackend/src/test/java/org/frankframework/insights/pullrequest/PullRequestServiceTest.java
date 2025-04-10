package org.frankframework.insights.pullrequest;

import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.common.configuration.GitHubProperties;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequestRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.mapper.MappingException;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubClientException;
import org.frankframework.insights.issue.IssueService;
import org.frankframework.insights.label.LabelService;
import org.frankframework.insights.milestone.MilestoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PullRequestServiceTest {

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private Mapper mapper;

    @Mock
    private BranchPullRequestRepository branchPullRequestRepository;

    @Mock
    private BranchService branchService;

    @Mock
    private LabelService labelService;

    @Mock
    private MilestoneService milestoneService;

    @Mock
    private IssueService issueService;

    @Mock
    private GitHubProperties gitHubProperties;

    @InjectMocks
    private PullRequestService pullRequestService;

    private Branch testBranch;
    private PullRequestDTO masterPR, branchPR;
    private PullRequest pullRequestEntity;

    @BeforeEach
    public void setUp() {
        testBranch = new Branch();
        testBranch.setId(UUID.randomUUID().toString());
        testBranch.setName("feature/abc");

        masterPR = new PullRequestDTO(
                UUID.randomUUID().toString(),
                1,
                "pr1",
                null,
                OffsetDateTime.now().minusDays(1),
                null,
                null,
                null);
        branchPR = new PullRequestDTO(
                UUID.randomUUID().toString(), 2, "pr2", null, OffsetDateTime.now(), null, null, null);

        pullRequestEntity = new PullRequest();
        pullRequestEntity.setTitle("pr2");

        List<String> branchProtectionRegexes = List.of("master", "release");

        when(gitHubProperties.getBranchProtectionRegexes()).thenReturn(branchProtectionRegexes);

        pullRequestService = new PullRequestService(
                gitHubClient,
                mapper,
                branchPullRequestRepository,
                branchService,
                labelService,
                milestoneService,
                issueService,
                gitHubProperties);
    }

    @Test
    public void injectBranchPullRequests_shouldUpdateBranchWithMergedPRs()
            throws PullRequestInjectionException, GitHubClientException, MappingException {
        when(branchService.getAllBranches()).thenReturn(List.of(testBranch));
        when(gitHubClient.getBranchPullRequests("master")).thenReturn(Set.of(masterPR));
        when(gitHubClient.getBranchPullRequests("feature/abc")).thenReturn(Set.of(branchPR));
        when(mapper.toEntity(anySet(), eq(PullRequest.class))).thenReturn(Set.of(pullRequestEntity));
        when(branchPullRequestRepository.findBranchPullRequestByBranchId(testBranch.getId()))
                .thenReturn(Collections.emptySet());

        pullRequestService.injectBranchPullRequests();

        verify(branchService).saveBranches(anySet());
    }

    @Test
    public void injectBranchPullRequests_shouldHandleEmptyBranchesList() throws PullRequestInjectionException {
        when(branchService.getAllBranches()).thenReturn(Collections.emptyList());

        pullRequestService.injectBranchPullRequests();

        verify(branchService, never()).saveBranches(anySet());
    }

    @Test
    public void injectBranchPullRequests_shouldHandleEmptyMasterPRs()
            throws PullRequestInjectionException, GitHubClientException, MappingException {
        when(branchService.getAllBranches()).thenReturn(List.of(testBranch));
        when(gitHubClient.getBranchPullRequests("master")).thenReturn(Collections.emptySet());
        when(gitHubClient.getBranchPullRequests("feature/abc")).thenReturn(Set.of(branchPR));
        when(mapper.toEntity(anySet(), eq(PullRequest.class))).thenReturn(Set.of(pullRequestEntity));
        when(branchPullRequestRepository.findBranchPullRequestByBranchId(testBranch.getId()))
                .thenReturn(Collections.emptySet());

        pullRequestService.injectBranchPullRequests();

        verify(branchService).saveBranches(anySet());
    }

    @Test
    public void injectBranchPullRequests_shouldSkipBranchOnException()
            throws PullRequestInjectionException, GitHubClientException {
        Branch masterBranch = new Branch();
        masterBranch.setId(UUID.randomUUID().toString());
        masterBranch.setName("master");

        when(branchService.getAllBranches()).thenReturn(List.of(testBranch, masterBranch));
        when(gitHubClient.getBranchPullRequests(masterBranch.getName())).thenReturn(Set.of(masterPR));
        when(gitHubClient.getBranchPullRequests(testBranch.getName()))
                .thenThrow(new GitHubClientException("GitHub client error", null));

        pullRequestService.injectBranchPullRequests();

        verify(branchService, times(1)).saveBranches(anySet());
    }

    @Test
    public void injectBranchPullRequests_shouldIncludeRelevantMasterPRs()
            throws PullRequestInjectionException, MappingException, GitHubClientException {
        PullRequestDTO earlyMasterPR =
                new PullRequestDTO(null, 0, "pr0", null, OffsetDateTime.now().minusDays(2), null, null, null);
        when(branchService.getAllBranches()).thenReturn(List.of(testBranch));
        when(gitHubClient.getBranchPullRequests("master")).thenReturn(Set.of(earlyMasterPR, masterPR));
        when(gitHubClient.getBranchPullRequests("feature/abc")).thenReturn(Set.of(branchPR));
        when(mapper.toEntity(anySet(), eq(PullRequest.class))).thenAnswer(invocation -> {
            Set<PullRequestDTO> dtos = invocation.getArgument(0);
            return dtos.stream()
                    .map(dto -> {
                        PullRequest pr = new PullRequest();
                        pr.setId(dto.id());
                        return pr;
                    })
                    .collect(Collectors.toSet());
        });
        when(branchPullRequestRepository.findBranchPullRequestByBranchId(testBranch.getId()))
                .thenReturn(Collections.emptySet());

        pullRequestService.injectBranchPullRequests();

        verify(branchService).saveBranches(anySet());
    }

    @Test
    public void injectBranchPullRequests_shouldHandleNullMergedAtDates()
            throws PullRequestInjectionException, GitHubClientException, MappingException {
        PullRequestDTO noMergePR = new PullRequestDTO(null, 99, "prX", null, null, null, null, null);
        when(branchService.getAllBranches()).thenReturn(List.of(testBranch));
        when(gitHubClient.getBranchPullRequests("master")).thenReturn(Set.of(noMergePR));
        when(gitHubClient.getBranchPullRequests("feature/abc")).thenReturn(Set.of(branchPR));

        pullRequestService.injectBranchPullRequests();

        verify(branchService, never()).saveBranches(anySet());
    }

    @Test
    public void injectBranchPullRequests_shouldAvoidDuplicatePRs()
            throws PullRequestInjectionException, GitHubClientException, MappingException {
        PullRequestDTO duplicatePR = new PullRequestDTO(null, 2, "pr2", null, OffsetDateTime.now(), null, null, null);

        when(branchService.getAllBranches()).thenReturn(List.of(testBranch));
        when(gitHubClient.getBranchPullRequests("master")).thenReturn(Set.of(duplicatePR));
        when(gitHubClient.getBranchPullRequests("feature/abc")).thenReturn(Set.of(duplicatePR));

        when(mapper.toEntity(anySet(), eq(PullRequest.class))).thenReturn(Set.of(pullRequestEntity));
        when(branchPullRequestRepository.findBranchPullRequestByBranchId(testBranch.getId()))
                .thenReturn(Collections.emptySet());

        pullRequestService.injectBranchPullRequests();

        verify(branchService).saveBranches(anySet());
    }
}
