package org.frankframework.insights.release;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.*;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequest;
import org.frankframework.insights.common.entityconnection.releasepullrequest.ReleasePullRequest;
import org.frankframework.insights.common.entityconnection.releasepullrequest.ReleasePullRequestRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.*;
import org.frankframework.insights.pullrequest.PullRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class ReleaseServiceTest {

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private Mapper mapper;

    @Mock
    private ReleaseRepository releaseRepository;

    @Mock
    private BranchService branchService;

    @Mock
    private ReleasePullRequestRepository releasePullRequestRepository;

    @InjectMocks
    private ReleaseService releaseService;

    private ReleaseDTO dto1;
    private ReleaseDTO dto2;
    private ReleaseDTO dtoMalformed;
    private Release rel1;
    private Release rel2;
    private Branch masterBranch, featureBranch, noNameBranch;
    private PullRequest pr1;
    private BranchPullRequest branchPR1;

    @BeforeEach
    public void setUp() {
        masterBranch = new Branch();
        masterBranch.setId(UUID.randomUUID().toString());
        masterBranch.setName("master");

        featureBranch = new Branch();
        featureBranch.setId(UUID.randomUUID().toString());
        featureBranch.setName("feature/1.2");

        noNameBranch = new Branch();
        noNameBranch.setId(UUID.randomUUID().toString());
        noNameBranch.setName(null);

        dto1 = new ReleaseDTO("id1", "v1.0", "v1.0", OffsetDateTime.now().minusDays(10));
        dto2 = new ReleaseDTO("id2", "v1.1", "v1.1", OffsetDateTime.now());

        dtoMalformed =
                new ReleaseDTO("id3", "foo_bar", "foo_bar", OffsetDateTime.now().minusDays(1));

        rel1 = new Release();
        rel1.setTagName("v1.0");
        rel1.setPublishedAt(dto1.publishedAt());
        rel1.setBranch(masterBranch);

        rel2 = new Release();
        rel2.setTagName("v1.1");
        rel2.setPublishedAt(dto2.publishedAt());
        rel2.setBranch(masterBranch);

        pr1 = new PullRequest();
        pr1.setId(UUID.randomUUID().toString());
        pr1.setMergedAt(rel1.getPublishedAt().plusDays(2));

        branchPR1 = new BranchPullRequest(masterBranch, pr1);
    }

    @Test
    public void injects_whenDatabaseEmpty() throws Exception {
        when(gitHubClient.getReleases()).thenReturn(Set.of(dto1));
        when(branchService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(rel1);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel1));
        when(branchService.getBranchPullRequestsByBranches(anyList()))
                .thenReturn(Map.of(masterBranch.getId(), Set.of(branchPR1)));

        releaseService.injectReleases();

        verify(releaseRepository).saveAll(anySet());
        verify(releasePullRequestRepository, atLeast(0)).saveAll(anySet());
    }

    @Test
    public void doesNothing_whenNoValidReleases() throws Exception {
        when(gitHubClient.getReleases()).thenReturn(Collections.emptySet());
        releaseService.injectReleases();
        verify(releaseRepository, never()).saveAll(anySet());
    }

    @Test
    public void fallbackToMasterBranch_whenNoVersionBranchMatches() throws Exception {
        ReleaseDTO dto = new ReleaseDTO("id", "v9.9", "v9.9", OffsetDateTime.now());

        Release rel = new Release();
        rel.setTagName("v9.9");
        rel.setPublishedAt(dto.publishedAt());
        rel.setBranch(masterBranch);

        when(gitHubClient.getReleases()).thenReturn(Set.of(dto));
        when(branchService.getAllBranches()).thenReturn(List.of(masterBranch, featureBranch, masterBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(rel);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel));
        when(branchService.getBranchPullRequestsByBranches(anyList())).thenReturn(Collections.emptyMap());

        releaseService.injectReleases();

        verify(releaseRepository).saveAll(anySet());
        verify(releasePullRequestRepository, never()).saveAll(anySet());
    }

    @Test
    public void nullBranch_ifNoMatchAndNoMaster() throws Exception {
        ReleaseDTO dto = new ReleaseDTO("id", "v999.999", "v999.9990", OffsetDateTime.now());

        Release rel = new Release();
        rel.setTagName("v999.999");
        rel.setPublishedAt(dto.publishedAt());
        rel.setBranch(null);

        when(gitHubClient.getReleases()).thenReturn(Set.of(dto));
        when(branchService.getAllBranches()).thenReturn(List.of(masterBranch, featureBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(rel);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel));
        when(branchService.getBranchPullRequestsByBranches(anyList())).thenReturn(Collections.emptyMap());

        releaseService.injectReleases();

        verify(releaseRepository).saveAll(anySet());
    }

    @Test
    public void assignsPullRequestsToCorrectReleaseByTimeframe() throws Exception {
        when(gitHubClient.getReleases()).thenReturn(Set.of(dto1, dto2));
        when(branchService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(eq(dto1), eq(Release.class))).thenReturn(rel1);
        when(mapper.toEntity(eq(dto2), eq(Release.class))).thenReturn(rel2);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel1, rel2));
        when(branchService.getBranchPullRequestsByBranches(anyList()))
                .thenReturn(Map.of(masterBranch.getId(), Set.of(branchPR1)));

        releaseService.injectReleases();

        verify(releasePullRequestRepository, atLeastOnce()).saveAll(anySet());
    }

    @Test
    public void assignsNothing_whenNoMatchingBranches() throws Exception {
        when(gitHubClient.getReleases()).thenReturn(Set.of(dto1));
        when(branchService.getAllBranches()).thenReturn(Collections.emptyList());
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(rel1);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel1));

        releaseService.injectReleases();

        verify(releaseRepository).saveAll(anySet());
        verify(releasePullRequestRepository, never()).saveAll(anySet());
    }

    @Test
    public void handlesNullBranchNameGracefully() throws Exception {
        Release relWithNull = new Release();
        relWithNull.setTagName("vX.Y");
        relWithNull.setPublishedAt(OffsetDateTime.now());
        relWithNull.setBranch(noNameBranch);

        ReleaseDTO dto = new ReleaseDTO("id", "vX.Y", "vX.Y", relWithNull.getPublishedAt());

        when(gitHubClient.getReleases()).thenReturn(Set.of(dto));
        when(branchService.getAllBranches()).thenReturn(List.of(noNameBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(relWithNull);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(relWithNull));
        when(branchService.getBranchPullRequestsByBranches(anyList()))
                .thenReturn(Map.of(noNameBranch.getId(), Set.of()));

        releaseService.injectReleases();

        verify(releaseRepository).saveAll(anySet());
    }

    @Test
    public void noPullRequestsAssigned_ifNoneInTimeWindow() throws Exception {
        pr1.setMergedAt(OffsetDateTime.now().plusYears(5));
        branchPR1 = new BranchPullRequest(masterBranch, pr1);

        when(gitHubClient.getReleases()).thenReturn(Set.of(dto1, dto2));
        when(branchService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(eq(dto1), eq(Release.class))).thenReturn(rel1);
        when(mapper.toEntity(eq(dto2), eq(Release.class))).thenReturn(rel2);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel1, rel2));
        when(branchService.getBranchPullRequestsByBranches(anyList()))
                .thenReturn(Map.of(masterBranch.getId(), Set.of(branchPR1)));

        releaseService.injectReleases();

        verify(releasePullRequestRepository, never()).saveAll(anySet());
    }

    @Test
    public void masterBranchWithReleases_assignsPRsToMaster() throws Exception {
        when(gitHubClient.getReleases()).thenReturn(Set.of(dto1));
        when(branchService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(rel1);
        rel1.setBranch(masterBranch);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel1));
        when(branchService.getBranchPullRequestsByBranches(anyList()))
                .thenReturn(Map.of(masterBranch.getId(), Set.of(branchPR1)));

        releaseService.injectReleases();

        verify(releaseRepository).saveAll(anySet());
        verify(releasePullRequestRepository, atLeast(0)).saveAll(anySet());
    }

    @Test
    public void malformedTagName_fallsBackToMasterOrNull() throws Exception {
        Release relMalformed = new Release();
        relMalformed.setTagName("foo_bar");
        relMalformed.setPublishedAt(dtoMalformed.publishedAt());
        relMalformed.setBranch(masterBranch);

        when(gitHubClient.getReleases()).thenReturn(Set.of(dtoMalformed));
        when(branchService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(relMalformed);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(relMalformed));
        when(branchService.getBranchPullRequestsByBranches(anyList())).thenReturn(Collections.emptyMap());

        releaseService.injectReleases();
        verify(releaseRepository).saveAll(anySet());
    }

    @Test
    public void throwsReleaseInjectionException_onGitHubClientException() throws GitHubClientException {
        when(gitHubClient.getReleases()).thenThrow(new GitHubClientException("fail", null));
        assertThrows(ReleaseInjectionException.class, () -> releaseService.injectReleases());
    }

    @Test
    public void getAllReleases_returnsAll() {
        ReleaseResponse resp1 = mock(ReleaseResponse.class);
        ReleaseResponse resp2 = mock(ReleaseResponse.class);
        when(releaseRepository.findAll()).thenReturn(List.of(rel1, rel2));
        when(mapper.toDTO(rel1, ReleaseResponse.class)).thenReturn(resp1);
        when(mapper.toDTO(rel2, ReleaseResponse.class)).thenReturn(resp2);

        Set<ReleaseResponse> result = releaseService.getAllReleases();
        assertEquals(Set.of(resp1, resp2), result);
    }

    @Test
    public void checkIfReleaseExists_returnsRelease() throws ReleaseNotFoundException {
        when(releaseRepository.findById("id1")).thenReturn(Optional.of(rel1));
        Release found = releaseService.checkIfReleaseExists("id1");
        assertEquals(rel1, found);
    }

    @Test
    public void checkIfReleaseExists_throwsIfNotFound() {
        when(releaseRepository.findById("id2")).thenReturn(Optional.empty());
        assertThrows(ReleaseNotFoundException.class, () -> releaseService.checkIfReleaseExists("id2"));
    }

    @Test
    public void extractMajorMinor_variousCases() throws Exception {
        ReleaseDTO tagGood = new ReleaseDTO("id", "v3.5", "v3.5", OffsetDateTime.now());
        Release relGood = new Release();
        relGood.setTagName("v3.5");
        relGood.setPublishedAt(tagGood.publishedAt());
        relGood.setBranch(masterBranch);

        ReleaseDTO tagBad = new ReleaseDTO("id", "vX.Y", "vX.Y", OffsetDateTime.now());
        Release relBad = new Release();
        relBad.setTagName("vX.Y");
        relBad.setPublishedAt(tagBad.publishedAt());
        relBad.setBranch(masterBranch);

        when(gitHubClient.getReleases()).thenReturn(Set.of(tagGood, tagBad));
        when(branchService.getAllBranches()).thenReturn(List.of(masterBranch, masterBranch, featureBranch));
        when(mapper.toEntity(eq(tagGood), eq(Release.class))).thenReturn(relGood);
        when(mapper.toEntity(eq(tagBad), eq(Release.class))).thenReturn(relBad);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(relGood, relBad));
        when(branchService.getBranchPullRequestsByBranches(anyList())).thenReturn(Collections.emptyMap());

        releaseService.injectReleases();
        verify(releaseRepository).saveAll(anySet());
    }

    @Test
    public void assignToReleases_ShouldAssignPRsToCorrectReleases_WhenPRsFallInTimeWindows() {
        Release r1 = new Release();
        r1.setId("r1");
        r1.setName("v1.0");
        r1.setPublishedAt(OffsetDateTime.parse("2025-08-01T10:00:00Z"));

        Release r2 = new Release();
        r2.setId("r2");
        r2.setName("v1.1");
        r2.setPublishedAt(OffsetDateTime.parse("2025-08-10T10:00:00Z"));

        Release r3 = new Release();
        r3.setId("r3");
        r3.setName("v1.2");
        r3.setPublishedAt(OffsetDateTime.parse("2025-08-20T10:00:00Z"));

        List<Release> releases = new ArrayList<>(List.of(r2, r1, r3)); // Unordered

        PullRequest pull1 = new PullRequest();
        pull1.setNumber(101);
        pull1.setMergedAt(OffsetDateTime.parse("2025-08-05T12:00:00Z"));
        BranchPullRequest pr1 = new BranchPullRequest(null, pull1);

        PullRequest pull2 = new PullRequest();
        pull2.setNumber(102);
        pull2.setMergedAt(OffsetDateTime.parse("2025-08-15T12:00:00Z"));
        BranchPullRequest pr2 = new BranchPullRequest(null, pull2);

        PullRequest pull3 = new PullRequest();
        pull3.setNumber(103);
        pull3.setMergedAt(OffsetDateTime.parse("2025-07-30T12:00:00Z"));
        BranchPullRequest pr3 = new BranchPullRequest(null, pull3);

        Set<BranchPullRequest> prs = new HashSet<>(Set.of(pr1, pr2, pr3));
        ArgumentCaptor<Set<ReleasePullRequest>> captor = ArgumentCaptor.forClass(Set.class);

        ReflectionTestUtils.invokeMethod(releaseService, "assignToReleases", releases, prs);

        verify(releasePullRequestRepository, times(2)).saveAll(captor.capture());
        List<Set<ReleasePullRequest>> capturedValues = captor.getAllValues();

        Set<ReleasePullRequest> release2Pulls = capturedValues.getFirst();
        assertEquals(1, release2Pulls.size());
        ReleasePullRequest rpr1 = release2Pulls.iterator().next();
        assertEquals("r2", rpr1.getRelease().getId());
        assertEquals(101, rpr1.getPullRequest().getNumber());

        Set<ReleasePullRequest> release3Pulls = capturedValues.get(1);
        assertEquals(1, release3Pulls.size());
        ReleasePullRequest rpr2 = release3Pulls.iterator().next();
        assertEquals("r3", rpr2.getRelease().getId());
        assertEquals(102, rpr2.getPullRequest().getNumber());
    }

    @Test
    public void assignToReleases_ShouldSortNightlyReleaseToEnd_AndAssignPRsBasedOnSortedOrder() {
        Release r1 = new Release();
        r1.setId("r1");
        r1.setName("v1.0");
        r1.setPublishedAt(OffsetDateTime.parse("2025-08-01T10:00:00Z"));

        Release r2Nightly = new Release();
        r2Nightly.setId("r2");
        r2Nightly.setName("My Nightly Release");
        r2Nightly.setPublishedAt(OffsetDateTime.parse("2025-08-15T10:00:00Z"));

        Release r3 = new Release();
        r3.setId("r3");
        r3.setName("v1.1");
        r3.setPublishedAt(OffsetDateTime.parse("2025-08-10T10:00:00Z"));

        List<Release> releases = new ArrayList<>(List.of(r2Nightly, r1, r3));

        PullRequest pull1 = new PullRequest();
        pull1.setNumber(101);
        pull1.setMergedAt(OffsetDateTime.parse("2025-08-08T12:00:00Z"));
        BranchPullRequest pr1 = new BranchPullRequest(null, pull1);

        Set<BranchPullRequest> prs = new HashSet<>(Set.of(pr1));
        ArgumentCaptor<Set<ReleasePullRequest>> captor = ArgumentCaptor.forClass(Set.class);

        ReflectionTestUtils.invokeMethod(releaseService, "assignToReleases", releases, prs);

        verify(releasePullRequestRepository, times(1)).saveAll(captor.capture());
        Set<ReleasePullRequest> release3Pulls = captor.getValue();
        assertEquals(1, release3Pulls.size());
        ReleasePullRequest rpr1 = release3Pulls.iterator().next();
        assertEquals("r3", rpr1.getRelease().getId());
        assertEquals(101, rpr1.getPullRequest().getNumber());
    }

    @Test
    public void assignToReleases_ShouldNotAssignAnything_WhenNoPRsAreInRange() {
        Release r1 = new Release();
        r1.setId("r1");
        r1.setName("v1.0");
        r1.setPublishedAt(OffsetDateTime.parse("2025-08-01T10:00:00Z"));

        Release r2 = new Release();
        r2.setId("r2");
        r2.setName("v1.1");
        r2.setPublishedAt(OffsetDateTime.parse("2025-08-10T10:00:00Z"));

        List<Release> releases = new ArrayList<>(List.of(r1, r2));

        PullRequest pull1 = new PullRequest();
        pull1.setNumber(101);
        pull1.setMergedAt(OffsetDateTime.parse("2025-08-15T12:00:00Z"));
        BranchPullRequest pr1 = new BranchPullRequest(null, pull1);

        Set<BranchPullRequest> prs = new HashSet<>(Set.of(pr1));

        ReflectionTestUtils.invokeMethod(releaseService, "assignToReleases", releases, prs);

        verify(releasePullRequestRepository, never()).saveAll(any());
    }
}
