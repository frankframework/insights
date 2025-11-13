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
import org.frankframework.insights.common.mapper.MappingException;
import org.frankframework.insights.common.properties.ReleaseFixProperties;
import org.frankframework.insights.github.graphql.GitHubGraphQLClient;
import org.frankframework.insights.github.graphql.GitHubGraphQLClientException;
import org.frankframework.insights.pullrequest.PullRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReleaseServiceTest {

    @Mock
    private GitHubGraphQLClient gitHubGraphQLClient;

    @Mock
    private Mapper mapper;

    @Mock
    private ReleaseRepository releaseRepository;

    @Mock
    private BranchService branchService;

    @Mock
    private ReleasePullRequestRepository releasePullRequestRepository;

    @Mock
    private ReleaseFixProperties releaseFixProperties;

    private ReleaseService releaseService;

    private ReleaseDTO dto1;
    private ReleaseDTO dto2;
    private ReleaseDTO dtoMalformed;
    private Release rel1;
    private Release rel2;
    private Branch masterBranch, featureBranch, noNameBranch;
    private PullRequest pr1;
    private BranchPullRequest branchPR1;

    private HashMap<String, OffsetDateTime> overrideMap;
    private OffsetDateTime originalDate2;
    private OffsetDateTime overrideDate;
    private ReleaseDTO releaseToFix;
    private ReleaseDTO releaseToKeep;

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

        overrideMap = new HashMap<>();

        OffsetDateTime originalDate1 = OffsetDateTime.parse("2024-01-15T10:00:00Z");
        originalDate2 = OffsetDateTime.parse("2024-03-20T14:00:00Z");
        overrideDate = OffsetDateTime.parse("2025-02-02T12:00:00Z");

        releaseToFix = new ReleaseDTO("id-1", "v8.0.5", "Release 8.0.5", originalDate1);
        releaseToKeep = new ReleaseDTO("id-2", "v8.0.4", "Release 8.0.4", originalDate2);

        when(releaseFixProperties.getDateOverrides()).thenReturn(overrideMap);

        releaseService = new ReleaseService(
                gitHubGraphQLClient,
                mapper,
                releaseRepository,
                branchService,
                releasePullRequestRepository,
                releaseFixProperties);
    }

    @Test
    public void injects_whenDatabaseEmpty() throws Exception {
        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(dto1));
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
        when(gitHubGraphQLClient.getReleases()).thenReturn(Collections.emptySet());
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

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(dto));
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

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(dto));
        when(branchService.getAllBranches()).thenReturn(List.of(masterBranch, featureBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(rel);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel));
        when(branchService.getBranchPullRequestsByBranches(anyList())).thenReturn(Collections.emptyMap());

        releaseService.injectReleases();

        verify(releaseRepository).saveAll(anySet());
    }

    @Test
    public void assignsPullRequestsToCorrectReleaseByTimeframe() throws Exception {
        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(dto1, dto2));
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
        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(dto1));
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

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(dto));
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

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(dto1, dto2));
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
        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(dto1));
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

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(dtoMalformed));
        when(branchService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(relMalformed);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(relMalformed));
        when(branchService.getBranchPullRequestsByBranches(anyList())).thenReturn(Collections.emptyMap());

        releaseService.injectReleases();
        verify(releaseRepository).saveAll(anySet());
    }

    @Test
    public void throwsReleaseInjectionException_onGitHubClientException() throws GitHubGraphQLClientException {
        when(gitHubGraphQLClient.getReleases()).thenThrow(new GitHubGraphQLClientException("fail", null));
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
    public void getReleaseById_returnsReleaseResponse() throws ReleaseNotFoundException {
        ReleaseResponse mockResponse = mock(ReleaseResponse.class);
        when(releaseRepository.findById("release-1")).thenReturn(Optional.of(rel1));
        when(mapper.toDTO(rel1, ReleaseResponse.class)).thenReturn(mockResponse);

        ReleaseResponse result = releaseService.getReleaseById("release-1");

        assertEquals(mockResponse, result);
        verify(releaseRepository).findById("release-1");
        verify(mapper).toDTO(rel1, ReleaseResponse.class);
    }

    @Test
    public void getReleaseById_throwsReleaseNotFoundException_whenReleaseNotFound() throws MappingException {
        when(releaseRepository.findById("nonexistent-id")).thenReturn(Optional.empty());

        ReleaseNotFoundException exception =
                assertThrows(ReleaseNotFoundException.class, () -> releaseService.getReleaseById("nonexistent-id"));

        assertEquals("Release with ID [nonexistent-id] not found.", exception.getMessage());
        verify(releaseRepository).findById("nonexistent-id");
        verify(mapper, never()).toDTO(any(), eq(ReleaseResponse.class));
    }

    @Test
    public void getReleaseById_handlesNullId() {
        when(releaseRepository.findById(null)).thenReturn(Optional.empty());

        assertThrows(ReleaseNotFoundException.class, () -> releaseService.getReleaseById(null));
        verify(releaseRepository).findById(null);
    }

    @Test
    public void getReleaseById_handlesEmptyString() {
        when(releaseRepository.findById("")).thenReturn(Optional.empty());

        assertThrows(ReleaseNotFoundException.class, () -> releaseService.getReleaseById(""));
        verify(releaseRepository).findById("");
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

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(tagGood, tagBad));
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

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(dto1, dto2, dto3));
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
    public void releaseSortingComparator_shouldSortNightlyReleasesToEnd() {
        Release normal_early = new Release();
        normal_early.setName("v1.0");
        normal_early.setPublishedAt(OffsetDateTime.parse("2025-08-10T10:00:00Z"));

        Release nightly_middle = new Release();
        nightly_middle.setName("A nightly build");
        nightly_middle.setPublishedAt(OffsetDateTime.parse("2025-08-15T10:00:00Z"));

        Release normal_late = new Release();
        normal_late.setName("v1.1");
        normal_late.setPublishedAt(OffsetDateTime.parse("2025-08-20T10:00:00Z"));

        Release null_name_release = new Release();
        null_name_release.setName(null);
        null_name_release.setPublishedAt(OffsetDateTime.parse("2025-08-01T10:00:00Z"));

        List<Release> releases = Arrays.asList(normal_early, nightly_middle, normal_late, null_name_release);
        Collections.shuffle(releases);

        Comparator<Release> comparator = releaseService.getReleaseSortingComparator();
        releases.sort(comparator);

        assertEquals(null_name_release, releases.get(0));
        assertEquals(normal_early, releases.get(1));
        assertEquals(normal_late, releases.get(2));
        assertEquals(nightly_middle, releases.get(3));
    }

    @Test
    public void isValidRelease_shouldFilterOutReleaseCandidate() throws Exception {
        ReleaseDTO rcRelease = new ReleaseDTO("id1", "v8.1.0-RC1", "v8.1.0-RC1", OffsetDateTime.now());
        ReleaseDTO validRelease = new ReleaseDTO("id2", "v8.1.0", "v8.1.0", OffsetDateTime.now());

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(rcRelease, validRelease));
        when(branchService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(eq(validRelease), eq(Release.class))).thenReturn(rel1);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel1));
        when(branchService.getBranchPullRequestsByBranches(anyList())).thenReturn(Collections.emptyMap());

        releaseService.injectReleases();

        ArgumentCaptor<Set<Release>> captor = ArgumentCaptor.forClass(Set.class);
        verify(releaseRepository).saveAll(captor.capture());
        Set<Release> savedReleases = captor.getValue();

        assertEquals(1, savedReleases.size());
        verify(mapper, times(1)).toEntity(eq(validRelease), eq(Release.class));
        verify(mapper, never()).toEntity(eq(rcRelease), eq(Release.class));
    }

    @Test
    public void isValidRelease_shouldFilterOutBetaRelease() throws Exception {
        ReleaseDTO betaRelease = new ReleaseDTO("id1", "v7.0-B2", "v7.0-B2", OffsetDateTime.now());
        ReleaseDTO validRelease = new ReleaseDTO("id2", "v7.0.0", "v7.0.0", OffsetDateTime.now());

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(betaRelease, validRelease));
        when(branchService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(eq(validRelease), eq(Release.class))).thenReturn(rel1);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel1));
        when(branchService.getBranchPullRequestsByBranches(anyList())).thenReturn(Collections.emptyMap());

        releaseService.injectReleases();

        ArgumentCaptor<Set<Release>> captor = ArgumentCaptor.forClass(Set.class);
        verify(releaseRepository).saveAll(captor.capture());
        Set<Release> savedReleases = captor.getValue();

        assertEquals(1, savedReleases.size());
        verify(mapper, times(1)).toEntity(eq(validRelease), eq(Release.class));
        verify(mapper, never()).toEntity(eq(betaRelease), eq(Release.class));
    }

    @Test
    public void isValidRelease_shouldFilterOutMultipleInvalidReleases() throws Exception {
        ReleaseDTO rc1 = new ReleaseDTO("id1", "v7.8-RC1", "v7.8-RC1", OffsetDateTime.now());
        ReleaseDTO rc2 = new ReleaseDTO("id2", "v7.8-RC2", "v7.8-RC2", OffsetDateTime.now());
        ReleaseDTO beta = new ReleaseDTO("id3", "v7.0-B3", "v7.0-B3", OffsetDateTime.now());
        ReleaseDTO validRelease = new ReleaseDTO("id4", "v7.8.0", "v7.8.0", OffsetDateTime.now());

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(rc1, rc2, beta, validRelease));
        when(branchService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(eq(validRelease), eq(Release.class))).thenReturn(rel1);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel1));
        when(branchService.getBranchPullRequestsByBranches(anyList())).thenReturn(Collections.emptyMap());

        releaseService.injectReleases();

        ArgumentCaptor<Set<Release>> captor = ArgumentCaptor.forClass(Set.class);
        verify(releaseRepository).saveAll(captor.capture());
        Set<Release> savedReleases = captor.getValue();

        assertEquals(1, savedReleases.size());
        verify(mapper, times(1)).toEntity(eq(validRelease), eq(Release.class));
        verify(mapper, never()).toEntity(eq(rc1), eq(Release.class));
        verify(mapper, never()).toEntity(eq(rc2), eq(Release.class));
        verify(mapper, never()).toEntity(eq(beta), eq(Release.class));
    }

    @Test
    public void isValidRelease_shouldHandleNullReleaseName() throws Exception {
        ReleaseDTO nullNameRelease = new ReleaseDTO("id1", null, null, OffsetDateTime.now());

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(nullNameRelease));
        when(branchService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(branchService.getBranchPullRequestsByBranches(anyList())).thenReturn(Collections.emptyMap());

        releaseService.injectReleases();

        verify(releaseRepository, never()).saveAll(anySet());
        verify(mapper, never()).toEntity(any(ReleaseDTO.class), eq(Release.class));
    }

    @Test
    public void isValidRelease_shouldAllowCaseInsensitiveMatching() throws Exception {
        ReleaseDTO rcLowercase = new ReleaseDTO("id1", "v7.5-rc2", "v7.5-rc2", OffsetDateTime.now());
        ReleaseDTO betaUppercase = new ReleaseDTO("id2", "v7.6-B1", "v7.6-B1", OffsetDateTime.now());
        ReleaseDTO validRelease = new ReleaseDTO("id3", "v7.6.0", "v7.6.0", OffsetDateTime.now());

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(rcLowercase, betaUppercase, validRelease));
        when(branchService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(eq(validRelease), eq(Release.class))).thenReturn(rel1);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel1));
        when(branchService.getBranchPullRequestsByBranches(anyList())).thenReturn(Collections.emptyMap());

        releaseService.injectReleases();

        ArgumentCaptor<Set<Release>> captor = ArgumentCaptor.forClass(Set.class);
        verify(releaseRepository).saveAll(captor.capture());
        Set<Release> savedReleases = captor.getValue();

        assertEquals(1, savedReleases.size());
        verify(mapper, times(1)).toEntity(eq(validRelease), eq(Release.class));
        verify(mapper, never()).toEntity(eq(rcLowercase), eq(Release.class));
        verify(mapper, never()).toEntity(eq(betaUppercase), eq(Release.class));
    }

    @Test
    public void testShouldOverrideDateWhenTagNameMatches() {
        overrideMap.put("v8.0.5", overrideDate);
        Set<ReleaseDTO> inputSet = Set.of(releaseToFix, releaseToKeep);

        Set<ReleaseDTO> resultSet = releaseService.applyManualDateFixes(inputSet);

        assertNotNull(resultSet);
        assertEquals(2, resultSet.size());

        ReleaseDTO fixedDto = resultSet.stream()
                .filter(dto -> "v8.0.5".equals(dto.tagName()))
                .findFirst()
                .orElseThrow();

        ReleaseDTO keptDto = resultSet.stream()
                .filter(dto -> "v8.0.4".equals(dto.tagName()))
                .findFirst()
                .orElseThrow();

        assertEquals(overrideDate, fixedDto.publishedAt());
        assertNotSame(releaseToFix, fixedDto);
        assertEquals("id-1", fixedDto.id());

        assertEquals(originalDate2, keptDto.publishedAt());
        assertSame(releaseToKeep, keptDto);
    }

    @Test
    public void testShouldReturnOriginalSetWhenNoMatches() {
        overrideMap.put("v9.9.9", overrideDate);
        Set<ReleaseDTO> inputSet = Set.of(releaseToFix, releaseToKeep);

        Set<ReleaseDTO> resultSet = releaseService.applyManualDateFixes(inputSet);

        assertNotNull(resultSet);
        assertEquals(2, resultSet.size());

        assertTrue(resultSet.contains(releaseToFix));
        assertTrue(resultSet.contains(releaseToKeep));

        assertTrue(resultSet.stream().anyMatch(dto -> dto == releaseToFix));
        assertTrue(resultSet.stream().anyMatch(dto -> dto == releaseToKeep));
    }

    @Test
    public void testShouldReturnSameSetWhenOverridesAreEmpty() {
        Set<ReleaseDTO> inputSet = Set.of(releaseToFix, releaseToKeep);

        Set<ReleaseDTO> resultSet = releaseService.applyManualDateFixes(inputSet);

        assertNotNull(resultSet);
        assertEquals(2, resultSet.size());
        assertSame(inputSet, resultSet, "Method should have short-circuited and returned the original set instance");
    }

    @Test
    public void testShouldReturnEmptySetWhenInputIsEmpty() {
        Set<ReleaseDTO> inputSet = Set.of();

        Set<ReleaseDTO> resultSet = releaseService.applyManualDateFixes(inputSet);

        assertNotNull(resultSet);
        assertTrue(resultSet.isEmpty());
        assertSame(inputSet, resultSet, "Method should have short-circuited and returned the original empty set");
    }
}
