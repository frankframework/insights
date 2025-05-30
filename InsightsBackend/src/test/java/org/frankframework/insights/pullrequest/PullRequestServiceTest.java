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
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.mapper.MappingException;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubClientException;
import org.frankframework.insights.github.GitHubEdgesDTO;
import org.frankframework.insights.github.GitHubNodeDTO;
import org.frankframework.insights.github.GitHubPropertyState;
import org.frankframework.insights.issue.Issue;
import org.frankframework.insights.issue.IssueDTO;
import org.frankframework.insights.issue.IssueService;
import org.frankframework.insights.label.Label;
import org.frankframework.insights.label.LabelDTO;
import org.frankframework.insights.label.LabelService;
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
    private LabelService labelService;

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
        mockPullRequest.setId(subBranchPR.id());
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
                labelService);
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
        when(pullRequestRepository.saveAll(anySet())).thenReturn(List.of(mockPullRequest));
        when(labelService.getAllLabelsMap()).thenReturn(Map.of());
        when(issueService.getAllIssuesMap()).thenReturn(Map.of());

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
        when(labelService.getAllLabelsMap()).thenReturn(Map.of());
        when(issueService.getAllIssuesMap()).thenReturn(Map.of());

        assertDoesNotThrow(() -> pullRequestService.injectBranchPullRequests());
    }

    @Test
    public void injectBranchPullRequests_shouldHandleNullMergedAtDates()
            throws PullRequestInjectionException, GitHubClientException {
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
        when(labelService.getAllLabelsMap()).thenReturn(Map.of());
        when(issueService.getAllIssuesMap()).thenReturn(Map.of());

        pullRequestService.injectBranchPullRequests();

        verify(pullRequestRepository, times(1)).saveAll(anySet());
        verify(branchPullRequestRepository, times(1)).saveAll(anySet());
    }

    @Test
    public void injectBranchPullRequests_shouldSkipWhenNoPullRequests()
            throws GitHubClientException, PullRequestInjectionException {
        Branch emptyBranch = new Branch();
        emptyBranch.setId(UUID.randomUUID().toString());
        emptyBranch.setName("feature/empty");

        when(branchService.getAllBranches()).thenReturn(List.of(emptyBranch));
        when(gitHubClient.getBranchPullRequests("master")).thenReturn(Collections.emptySet());
        when(gitHubClient.getBranchPullRequests("feature/empty")).thenReturn(Collections.emptySet());

        pullRequestService.injectBranchPullRequests();

        verify(branchPullRequestRepository, never()).saveAll(anySet());
    }

    @Test
    public void injectBranchPullRequests_shouldHandleNullPRsAndSkip()
            throws GitHubClientException, PullRequestInjectionException {
        Branch branch = new Branch();
        branch.setId(UUID.randomUUID().toString());
        branch.setName("feature/nullpr");

        PullRequestDTO prWithNullDate = new PullRequestDTO("id-n", 42, "null pr", null, null, null, null, null);

        when(branchService.getAllBranches()).thenReturn(List.of(branch));
        when(gitHubClient.getBranchPullRequests("master")).thenReturn(Set.of(prWithNullDate));
        when(gitHubClient.getBranchPullRequests("feature/nullpr")).thenReturn(Collections.emptySet());

        pullRequestService.injectBranchPullRequests();

        verify(branchPullRequestRepository, never()).saveAll(anySet());
    }

    @Test
    public void injectBranchPullRequests_shouldSaveLabelsAndIssues()
            throws GitHubClientException, PullRequestInjectionException, MappingException {
        Branch branch = new Branch();
        branch.setId(UUID.randomUUID().toString());
        branch.setName("feature/labels");

        LabelDTO labelDTO = new LabelDTO();
        labelDTO.id = "l1";
        labelDTO.name = "bug";
        labelDTO.description = "desc";
        labelDTO.color = "red";

        GitHubNodeDTO<LabelDTO> labelNode = new GitHubNodeDTO<>();
        labelNode.setNode(labelDTO);
        List<GitHubNodeDTO<LabelDTO>> labelNodeList = List.of(labelNode);
        GitHubEdgesDTO<LabelDTO> labelEdges = new GitHubEdgesDTO<>();
        labelEdges.setEdges(labelNodeList);

        IssueDTO issue = new IssueDTO("i1", 1, "issue1", GitHubPropertyState.OPEN, null, null, null, null, null, null);
        GitHubNodeDTO<IssueDTO> issueNode = new GitHubNodeDTO<>();
        issueNode.setNode(issue);
        GitHubEdgesDTO<IssueDTO> closingIssuesEdges = new GitHubEdgesDTO<>();
        closingIssuesEdges.setEdges(List.of(issueNode));

        PullRequestDTO prDto = new PullRequestDTO(
                "id-x", 4, "pr labels", null, OffsetDateTime.now(), labelEdges, null, closingIssuesEdges);
        PullRequest prEntity = new PullRequest();
        prEntity.setId("id-x");
        prEntity.setTitle("pr labels");

        when(branchService.getAllBranches()).thenReturn(List.of(branch));
        when(gitHubClient.getBranchPullRequests("master")).thenReturn(Set.of(prDto));
        when(gitHubClient.getBranchPullRequests("feature/labels")).thenReturn(Set.of(prDto));
        when(branchPullRequestRepository.countAllByBranch_Id(any())).thenReturn(0);
        when(mapper.toEntity(anySet(), eq(PullRequest.class))).thenReturn(Set.of(prEntity));
        when(branchPullRequestRepository.findAllByBranch_Id(branch.getId())).thenReturn(Collections.emptySet());
        when(pullRequestRepository.saveAll(anySet())).thenReturn(List.of(prEntity));

        Label label = new Label();
        label.setId("l1");
        Map<String, Label> labelMap = Map.of("l1", label);
        when(labelService.getAllLabelsMap()).thenReturn(labelMap);

        // Make sure the issue service returns a map of issue id to Issue, not IssueDTO
        Issue issueEntity = new Issue();
        issueEntity.setId("i1");
        Map<String, Issue> issueMap = Map.of("i1", issueEntity);
        when(issueService.getAllIssuesMap()).thenReturn(issueMap);

        when(pullRequestLabelRepository.saveAll(anySet())).thenReturn(Collections.emptyList());
        when(pullRequestIssueRepository.saveAll(anySet())).thenReturn(Collections.emptyList());

        pullRequestService.injectBranchPullRequests();

        verify(pullRequestRepository, times(1)).saveAll(anySet());
        verify(branchPullRequestRepository, times(1)).saveAll(anySet());
        verify(pullRequestLabelRepository, atLeastOnce()).saveAll(anySet());
        verify(pullRequestIssueRepository, atLeastOnce()).saveAll(anySet());
    }

    @Test
    public void injectBranchPullRequests_shouldProcessBranchEvenIfExceptionInOne()
            throws GitHubClientException, PullRequestInjectionException, MappingException {
        Branch b1 = new Branch();
        b1.setId(UUID.randomUUID().toString());
        b1.setName("feature/b1");
        Branch b2 = new Branch();
        b2.setId(UUID.randomUUID().toString());
        b2.setName("feature/b2");

        PullRequestDTO dtoB1 = new PullRequestDTO("id-b1", 10, "b1", null, OffsetDateTime.now(), null, null, null);
        PullRequest prB1 = new PullRequest();
        prB1.setId("id-b1");
        prB1.setTitle("b1");

        when(branchService.getAllBranches()).thenReturn(List.of(b1, b2));
        when(gitHubClient.getBranchPullRequests("master")).thenReturn(Set.of(dtoB1));
        when(gitHubClient.getBranchPullRequests("feature/b1")).thenReturn(Set.of(dtoB1));
        when(gitHubClient.getBranchPullRequests("feature/b2")).thenThrow(new GitHubClientException("fail b2", null));

        when(branchPullRequestRepository.countAllByBranch_Id(any())).thenReturn(0);
        when(mapper.toEntity(anySet(), eq(PullRequest.class))).thenReturn(Set.of(prB1));
        when(branchPullRequestRepository.findAllByBranch_Id(b1.getId())).thenReturn(Collections.emptySet());
        when(pullRequestRepository.saveAll(anySet())).thenReturn(List.of(prB1));

        when(labelService.getAllLabelsMap()).thenReturn(Map.of());
        when(issueService.getAllIssuesMap()).thenReturn(Map.of());

        pullRequestService.injectBranchPullRequests();

        verify(branchPullRequestRepository, atLeastOnce()).saveAll(anySet());
    }

    @Test
    public void injectBranchPullRequests_shouldNotSaveIfNoMergedPRs()
            throws GitHubClientException, PullRequestInjectionException {
        Branch branch = new Branch();
        branch.setId(UUID.randomUUID().toString());
        branch.setName("emptypr");

        when(branchService.getAllBranches()).thenReturn(List.of(branch));
        when(gitHubClient.getBranchPullRequests("master")).thenReturn(Collections.emptySet());
        when(gitHubClient.getBranchPullRequests("emptypr")).thenReturn(Collections.emptySet());

        pullRequestService.injectBranchPullRequests();

        verify(branchPullRequestRepository, never()).saveAll(anySet());
    }

    @Test
    public void injectBranchPullRequests_shouldDoNothingIfBranchesEmpty() throws PullRequestInjectionException {
        when(branchService.getAllBranches()).thenReturn(Collections.emptyList());
        pullRequestService.injectBranchPullRequests();
        verifyNoInteractions(branchPullRequestRepository);
    }
}
