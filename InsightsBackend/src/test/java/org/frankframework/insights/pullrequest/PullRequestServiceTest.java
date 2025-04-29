package org.frankframework.insights.pullrequest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.*;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.common.configuration.properties.GitHubProperties;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequest;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequestRepository;
import org.frankframework.insights.common.entityconnection.pullrequestissue.PullRequestIssueRepository;
import org.frankframework.insights.common.entityconnection.pullrequestlabel.PullRequestLabelRepository;
import org.frankframework.insights.common.helper.IssueLabelHelperService;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.mapper.MappingException;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubClientException;
import org.frankframework.insights.issue.IssueService;
import org.frankframework.insights.milestone.MilestoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PullRequestServiceTest {

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private Mapper mapper;

    @Mock
    private PullRequestRepository pullRequestRepository;

    @Mock
    private BranchPullRequestRepository branchPullRequestRepository;

    @Mock
    private BranchService branchService;

    @Mock
    private MilestoneService milestoneService;

    @Mock
    private IssueService issueService;

    @Mock
    private GitHubProperties gitHubProperties;

    @Mock
    private PullRequestLabelRepository pullRequestLabelRepository;

    @Mock
    private PullRequestIssueRepository pullRequestIssueRepository;

    @Mock
    private IssueLabelHelperService issueLabelHelperService;

    @InjectMocks
    private PullRequestService pullRequestService;

    private Branch testBranch;
    private PullRequestDTO masterPR, subBranchPR;
    private PullRequest mockPullRequest;

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

        subBranchPR = new PullRequestDTO(
                UUID.randomUUID().toString(), 2, "pr2", null, OffsetDateTime.now(), null, null, null);

        mockPullRequest = new PullRequest();
        mockPullRequest.setTitle("pr2");

        List<String> branchProtectionRegexes = List.of("master", "release");

        when(gitHubProperties.getBranchProtectionRegexes()).thenReturn(branchProtectionRegexes);

        pullRequestService = new PullRequestService(
                gitHubClient,
                mapper,
                pullRequestRepository,
                branchPullRequestRepository,
                branchService,
                milestoneService,
                issueService,
                gitHubProperties,
                pullRequestLabelRepository,
                pullRequestIssueRepository,
                issueLabelHelperService);
    }

    @Test
    public void injectBranchPullRequests_shouldUpdateBranchWithMergedPRs()
            throws PullRequestInjectionException, GitHubClientException, MappingException {
        when(branchService.getAllBranches()).thenReturn(List.of(testBranch));
        when(gitHubClient.getBranchPullRequests("master")).thenReturn(Set.of(masterPR));
        when(gitHubClient.getBranchPullRequests("feature/abc")).thenReturn(Set.of(subBranchPR));
        when(branchPullRequestRepository.countAllByBranch_Id(any())).thenReturn(0);
        when(mapper.toEntity(anySet(), eq(PullRequest.class))).thenReturn(Set.of(mockPullRequest));
        when(branchPullRequestRepository.findAllByBranch_Id(testBranch.getId()))
                .thenReturn(Set.of(new BranchPullRequest(testBranch, mockPullRequest)));
        when(pullRequestRepository.saveAll(Set.of(mockPullRequest))).thenReturn(List.of(mockPullRequest));

        pullRequestService.injectBranchPullRequests();

        verify(branchPullRequestRepository, times(1)).saveAll(anySet());
    }

    @Test
    public void injectBranchPullRequests_shouldHandleEmptyBranchesList() throws PullRequestInjectionException {
        when(branchService.getAllBranches()).thenReturn(Collections.emptyList());

        pullRequestService.injectBranchPullRequests();

        verify(branchPullRequestRepository, never()).saveAll(anySet());
    }

    @Test
    public void injectBranchPullRequests_shouldThrowNothingIfGetPullRequestsFails() throws GitHubClientException {
        when(branchService.getAllBranches()).thenReturn(List.of(testBranch));
        when(gitHubClient.getBranchPullRequests("master")).thenReturn(Collections.singleton(subBranchPR));

        assertDoesNotThrow(() -> pullRequestService.injectBranchPullRequests());
    }

    @Test
    public void injectBranchPullRequests_shouldSkipBranchOnException() throws GitHubClientException {
        Branch masterBranch = new Branch();
        masterBranch.setId(UUID.randomUUID().toString());
        masterBranch.setName("master");

        when(branchService.getAllBranches()).thenReturn(List.of(testBranch, masterBranch));
        when(gitHubClient.getBranchPullRequests(masterBranch.getName())).thenReturn(Set.of(masterPR));
        when(gitHubClient.getBranchPullRequests(testBranch.getName()))
                .thenThrow(new GitHubClientException("GitHub client error", null));

        assertDoesNotThrow(() -> pullRequestService.injectBranchPullRequests());
    }

    @Test
    public void injectBranchPullRequests_shouldHandleNullMergedAtDates()
            throws PullRequestInjectionException, GitHubClientException, MappingException {
        PullRequestDTO noMergePR = new PullRequestDTO(null, 99, "prX", null, null, null, null, null);
        when(branchService.getAllBranches()).thenReturn(List.of(testBranch));
        when(gitHubClient.getBranchPullRequests("master")).thenReturn(Set.of(noMergePR));
        when(gitHubClient.getBranchPullRequests("feature/abc")).thenReturn(Set.of(subBranchPR));

        pullRequestService.injectBranchPullRequests();

        verify(branchPullRequestRepository, never()).saveAll(anySet());
    }

    @Test
    public void injectBranchPullRequests_shouldAvoidDuplicatePRs()
            throws PullRequestInjectionException, GitHubClientException, MappingException {
        PullRequestDTO duplicatePR = new PullRequestDTO("id-2", 2, "pr2", null, OffsetDateTime.now(), null, null, null);

        when(branchService.getAllBranches()).thenReturn(List.of(testBranch));
        when(gitHubClient.getBranchPullRequests("master")).thenReturn(Set.of(duplicatePR));
        when(gitHubClient.getBranchPullRequests("feature/abc")).thenReturn(Set.of(duplicatePR));
        when(branchPullRequestRepository.countAllByBranch_Id(any())).thenReturn(0);

        when(mapper.toEntity(anySet(), eq(PullRequest.class))).thenReturn(Set.of(mockPullRequest));
        when(branchPullRequestRepository.findAllByBranch_Id(testBranch.getId())).thenReturn(Collections.emptySet());

        when(pullRequestRepository.saveAll(anySet())).thenReturn(List.of(mockPullRequest));

        when(issueLabelHelperService.getAllLabelsMap()).thenReturn(Map.of());
        when(milestoneService.getAllMilestonesMap()).thenReturn(Map.of());
        when(issueService.getAllIssuesMap()).thenReturn(Map.of());

        pullRequestService.injectBranchPullRequests();

        verify(pullRequestRepository, times(1)).saveAll(anySet());
        verify(branchPullRequestRepository, times(1)).saveAll(anySet());
    }
}
