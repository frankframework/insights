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
    public void injectReleases_shouldAssignPRsToCorrectReleases_whenPRsFallInTimeWindows() throws Exception {
        // Arrange
        ReleaseDTO dto1 = new ReleaseDTO("id1", "v1.0", "v1.0", OffsetDateTime.parse("2025-08-01T10:00:00Z"));
        ReleaseDTO dto2 = new ReleaseDTO("id2", "v1.1", "v1.1", OffsetDateTime.parse("2025-08-10T10:00:00Z"));
        ReleaseDTO dto3 = new ReleaseDTO("id3", "v1.2", "v1.2", OffsetDateTime.parse("2025-08-20T10:00:00Z"));

        Release rel1 = new Release();
        rel1.setId("r1");
        rel1.setName("v1.0");
        rel1.setPublishedAt(dto1.publishedAt());
        rel1.setBranch(masterBranch);

        Release rel2 = new Release();
        rel2.setId("r2");
        rel2.setName("v1.1");
        rel2.setPublishedAt(dto2.publishedAt());
        rel2.setBranch(masterBranch);

        Release rel3 = new Release();
        rel3.setId("r3");
        rel3.setName("v1.2");
        rel3.setPublishedAt(dto3.publishedAt());
        rel3.setBranch(masterBranch);

        PullRequest pull1 = new PullRequest();
        pull1.setNumber(101);
        pull1.setMergedAt(OffsetDateTime.parse("2025-08-05T12:00:00Z"));
        BranchPullRequest bpr1 = new BranchPullRequest(masterBranch, pull1);

        PullRequest pull2 = new PullRequest();
        pull2.setNumber(102);
        pull2.setMergedAt(OffsetDateTime.parse("2025-08-15T12:00:00Z"));
        BranchPullRequest bpr2 = new BranchPullRequest(masterBranch, pull2);

        when(gitHubClient.getReleases()).thenReturn(Set.of(dto1, dto2, dto3));
        when(branchService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenAnswer(invocation -> {
            ReleaseDTO dto = invocation.getArgument(0);
            return switch (dto.id()) {
                case "id1" -> rel1;
                case "id2" -> rel2;
                case "id3" -> rel3;
                default -> null;
            };
        });
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel1, rel2, rel3));
        when(branchService.getBranchPullRequestsByBranches(anyList()))
                .thenReturn(Map.of(masterBranch.getId(), Set.of(bpr1, bpr2)));

        ArgumentCaptor<Set<ReleasePullRequest>> captor = ArgumentCaptor.forClass(Set.class);

        releaseService.injectReleases();

        verify(releasePullRequestRepository, times(2)).saveAll(captor.capture());
        List<Set<ReleasePullRequest>> capturedValues = captor.getAllValues();

        Set<ReleasePullRequest> release2Pulls = capturedValues.stream()
                .filter(s -> s.iterator().next().getRelease().getId().equals("r2"))
                .findFirst()
                .orElseThrow();
        Set<ReleasePullRequest> release3Pulls = capturedValues.stream()
                .filter(s -> s.iterator().next().getRelease().getId().equals("r3"))
                .findFirst()
                .orElseThrow();

        assertEquals(1, release2Pulls.size());
        assertEquals(101, release2Pulls.iterator().next().getPullRequest().getNumber());

        assertEquals(1, release3Pulls.size());
        assertEquals(102, release3Pulls.iterator().next().getPullRequest().getNumber());
    }

    @Test
    public void injectReleases_shouldSortNightlyReleaseToEnd() throws Exception {
        // Arrange
        ReleaseDTO dto1 = new ReleaseDTO("id1", "v1.0", "v1.0", OffsetDateTime.parse("2025-08-01T10:00:00Z"));
        ReleaseDTO dto2Nightly =
                new ReleaseDTO("id2", "A Nightly Release", "nightly", OffsetDateTime.parse("2025-08-15T10:00:00Z"));
        ReleaseDTO dto3 = new ReleaseDTO("id3", "v1.1", "v1.1", OffsetDateTime.parse("2025-08-10T10:00:00Z"));

        Release rel1 = new Release();
        rel1.setId("r1");
        rel1.setName(dto1.name());
        rel1.setPublishedAt(dto1.publishedAt());
        rel1.setBranch(masterBranch);

        Release rel2Nightly = new Release();
        rel2Nightly.setId("r2");
        rel2Nightly.setName(dto2Nightly.name());
        rel2Nightly.setPublishedAt(dto2Nightly.publishedAt());
        rel2Nightly.setBranch(masterBranch);

        Release rel3 = new Release();
        rel3.setId("r3");
        rel3.setName(dto3.name());
        rel3.setPublishedAt(dto3.publishedAt());
        rel3.setBranch(masterBranch);

        PullRequest pull1 = new PullRequest();
        pull1.setNumber(101);
        pull1.setMergedAt(OffsetDateTime.parse("2025-08-08T12:00:00Z"));
        BranchPullRequest bpr1 = new BranchPullRequest(masterBranch, pull1);

        when(gitHubClient.getReleases()).thenReturn(Set.of(dto1, dto2Nightly, dto3));
        when(branchService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenAnswer(inv -> {
            ReleaseDTO dto = inv.getArgument(0);
            if (dto.id().equals("id1")) return rel1;
            if (dto.id().equals("id2")) return rel2Nightly;
            return rel3;
        });
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel1, rel2Nightly, rel3));
        when(branchService.getBranchPullRequestsByBranches(anyList()))
                .thenReturn(Map.of(masterBranch.getId(), Set.of(bpr1)));
        ArgumentCaptor<Set<ReleasePullRequest>> captor = ArgumentCaptor.forClass(Set.class);

        releaseService.injectReleases();

        verify(releasePullRequestRepository, times(1)).saveAll(captor.capture());
        Set<ReleasePullRequest> savedPulls = captor.getValue();
        assertEquals(1, savedPulls.size());
        assertEquals("r3", savedPulls.iterator().next().getRelease().getId());
    }

    @Test
    public void injectReleases_shouldNotAssignAnything_whenNoPRsAreInRange() throws Exception {
        ReleaseDTO dto1 = new ReleaseDTO("id1", "v1.0", "v1.0", OffsetDateTime.parse("2025-08-01T10:00:00Z"));
        ReleaseDTO dto2 = new ReleaseDTO("id2", "v1.1", "v1.1", OffsetDateTime.parse("2025-08-10T10:00:00Z"));

        Release rel1 = new Release();
        rel1.setPublishedAt(dto1.publishedAt());
        rel1.setBranch(masterBranch);

        Release rel2 = new Release();
        rel2.setPublishedAt(dto2.publishedAt());
        rel2.setBranch(masterBranch);

        PullRequest pull1 = new PullRequest();
        pull1.setMergedAt(OffsetDateTime.parse("2025-08-15T12:00:00Z"));
        BranchPullRequest bpr1 = new BranchPullRequest(masterBranch, pull1);

        when(gitHubClient.getReleases()).thenReturn(Set.of(dto1, dto2));
        when(branchService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenAnswer(invocation -> {
            ReleaseDTO dto = invocation.getArgument(0);
            return switch (dto.id()) {
                case "id1" -> rel1;
                case "id2" -> rel2;
                default -> null;
            };
        });
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel1, rel2));
        when(branchService.getBranchPullRequestsByBranches(anyList()))
                .thenReturn(Map.of(masterBranch.getId(), Set.of(bpr1)));

        releaseService.injectReleases();

        verify(releasePullRequestRepository, never()).saveAll(any());
    }

	@Test
	public void injectReleases_shouldCorrectlySortReleases_includingNightlyAndNullNames() throws Exception {
		ReleaseDTO dtoNormal = new ReleaseDTO("id_normal", "v1.0", "v1.0", OffsetDateTime.parse("2025-08-10T10:00:00Z"));
		Release relNormal = new Release();
		relNormal.setId("rel_normal");
		relNormal.setName(dtoNormal.name());
		relNormal.setPublishedAt(dtoNormal.publishedAt());
		relNormal.setBranch(masterBranch);

		ReleaseDTO dtoNightly = new ReleaseDTO("id_nightly", "A nightly build", "nightly-tag", OffsetDateTime.parse("2025-08-01T10:00:00Z"));
		Release relNightly = new Release();
		relNightly.setId("rel_nightly");
		relNightly.setName(dtoNightly.name());
		relNightly.setPublishedAt(dtoNightly.publishedAt());
		relNightly.setBranch(masterBranch);

		ReleaseDTO dtoNullName = new ReleaseDTO("id_null", null, "null-name-tag", OffsetDateTime.parse("2025-08-20T10:00:00Z"));
		Release relNullName = new Release();
		relNullName.setId("rel_null");
		relNullName.setName(null);
		relNullName.setPublishedAt(dtoNullName.publishedAt());
		relNullName.setBranch(masterBranch);

		PullRequest pull = new PullRequest();
		pull.setNumber(101);
		pull.setMergedAt(OffsetDateTime.parse("2025-08-15T12:00:00Z"));
		BranchPullRequest bpr = new BranchPullRequest(masterBranch, pull);

		when(gitHubClient.getReleases()).thenReturn(Set.of(dtoNormal, dtoNightly, dtoNullName));
		when(branchService.getAllBranches()).thenReturn(List.of(masterBranch));
		when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenAnswer(inv -> {
			ReleaseDTO dto = inv.getArgument(0);
			if (dto.id().equals("id_normal")) return relNormal;
			if (dto.id().equals("id_nightly")) return relNightly;
			return relNullName;
		});
		when(releaseRepository.saveAll(anySet())).thenReturn(List.of(relNormal, relNightly, relNullName));
		when(branchService.getBranchPullRequestsByBranches(anyList())).thenReturn(Map.of(masterBranch.getId(), Set.of(bpr)));

		ArgumentCaptor<Set<ReleasePullRequest>> captor = ArgumentCaptor.forClass(Set.class);

		releaseService.injectReleases();

		verify(releasePullRequestRepository, times(1)).saveAll(captor.capture());

		Set<ReleasePullRequest> savedPulls = captor.getValue();

		assertEquals(1, savedPulls.size());
		assertEquals("rel_null", savedPulls.iterator().next().getRelease().getId());
	}

	@Test
	public void injectReleases_shouldCorrectlySortMixedReleases_toCoverComparatorLogic() throws Exception {
		Branch testBranch = new Branch();
		testBranch.setId("test-branch-id");
		testBranch.setName("master");

		ReleaseDTO dtoNormalLate = new ReleaseDTO("id_normal_late", "v1.1", "v1.1", OffsetDateTime.parse("2025-08-20T10:00:00Z"));
		Release relNormalLate = new Release();
		relNormalLate.setId("rel_normal_late");
		relNormalLate.setName(dtoNormalLate.name());
		relNormalLate.setPublishedAt(dtoNormalLate.publishedAt());
		relNormalLate.setBranch(testBranch);

		ReleaseDTO dtoNightly = new ReleaseDTO("id_nightly", "My nightly build", "nightly-tag", OffsetDateTime.parse("2025-08-15T10:00:00Z"));
		Release relNightly = new Release();
		relNightly.setId("rel_nightly");
		relNightly.setName(dtoNightly.name());
		relNightly.setPublishedAt(dtoNightly.publishedAt());
		relNightly.setBranch(testBranch);

		ReleaseDTO dtoNormalEarly = new ReleaseDTO("id_normal_early", "v1.0", "v1.0", OffsetDateTime.parse("2025-08-10T10:00:00Z"));
		Release relNormalEarly = new Release();
		relNormalEarly.setId("rel_normal_early");
		relNormalEarly.setName(dtoNormalEarly.name());
		relNormalEarly.setPublishedAt(dtoNormalEarly.publishedAt());
		relNormalEarly.setBranch(testBranch);

		PullRequest pull = new PullRequest();
		pull.setNumber(101);
		pull.setMergedAt(OffsetDateTime.parse("2025-08-18T12:00:00Z"));
		BranchPullRequest bpr = new BranchPullRequest(testBranch, pull);

		when(gitHubClient.getReleases()).thenReturn(Set.of(dtoNormalLate, dtoNightly, dtoNormalEarly));
		when(branchService.getAllBranches()).thenReturn(List.of(testBranch));
		when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenAnswer(inv -> {
			ReleaseDTO dto = inv.getArgument(0);
			if (dto.id().equals("id_normal_late")) return relNormalLate;
			if (dto.id().equals("id_nightly")) return relNightly;
			return relNormalEarly;
		});
		when(releaseRepository.saveAll(anySet())).thenReturn(List.of(relNormalLate, relNightly, relNormalEarly));
		when(branchService.getBranchPullRequestsByBranches(anyList())).thenReturn(Map.of(testBranch.getId(), Set.of(bpr)));

		ArgumentCaptor<Set<ReleasePullRequest>> captor = ArgumentCaptor.forClass(Set.class);

		releaseService.injectReleases();

		verify(releasePullRequestRepository, times(1)).saveAll(captor.capture());

		Set<ReleasePullRequest> savedPulls = captor.getValue();
		assertEquals(1, savedPulls.size());

		assertEquals("rel_normal_late", savedPulls.iterator().next().getRelease().getId());
	}
}
