package org.frankframework.insights.release;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.*;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchInjectionService;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequest;
import org.frankframework.insights.common.entityconnection.releasepullrequest.ReleasePullRequest;
import org.frankframework.insights.common.entityconnection.releasepullrequest.ReleasePullRequestRepository;
import org.frankframework.insights.common.entityconnection.releasevulnerability.ReleaseVulnerabilityRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.graphql.GitHubGraphQLClient;
import org.frankframework.insights.github.graphql.GitHubGraphQLClientException;
import org.frankframework.insights.pullrequest.PullRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReleaseInjectionServiceTest {
    @Mock
    private GitHubGraphQLClient gitHubGraphQLClient;

    @Mock
    private Mapper mapper;

    @Mock
    private ReleaseRepository releaseRepository;

    @Mock
    private BranchInjectionService branchInjectionService;

    @Mock
    private ReleasePullRequestRepository releasePullRequestRepository;

    @Mock
    private ReleaseVulnerabilityRepository releaseVulnerabilityRepository;

    private ReleaseInjectionService releaseInjectionService;

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

        dto1 = new ReleaseDTO(
                "id1",
                "v1.0",
                "v1.0",
                new ReleaseTagCommitDTO(OffsetDateTime.now().minusDays(10)));
        dto2 = new ReleaseDTO("id2", "v1.1", "v1.1", new ReleaseTagCommitDTO(OffsetDateTime.now()));

        dtoMalformed = new ReleaseDTO(
                "id3",
                "foo_bar",
                "foo_bar",
                new ReleaseTagCommitDTO(OffsetDateTime.now().minusDays(1)));

        rel1 = new Release();
        rel1.setId("id1");
        rel1.setTagName("v1.0");
        rel1.setPublishedAt(dto1.getReleaseDate());
        rel1.setBranch(masterBranch);

        rel2 = new Release();
        rel2.setId("id2");
        rel2.setTagName("v1.1");
        rel2.setPublishedAt(dto2.getReleaseDate());
        rel2.setBranch(masterBranch);

        pr1 = new PullRequest();
        pr1.setId(UUID.randomUUID().toString());
        pr1.setMergedAt(rel1.getPublishedAt().plusDays(2));

        branchPR1 = new BranchPullRequest(masterBranch, pr1);

        releaseInjectionService = new ReleaseInjectionService(
                gitHubGraphQLClient,
                mapper,
                releaseRepository,
                branchInjectionService,
                releasePullRequestRepository,
                releaseVulnerabilityRepository);
    }

    @Test
    public void injects_whenDatabaseEmpty() throws Exception {
        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(dto1));
        when(branchInjectionService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(rel1);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel1));
        when(branchInjectionService.getBranchPullRequestsByBranches(anyList()))
                .thenReturn(Map.of(masterBranch.getId(), Set.of(branchPR1)));

        releaseInjectionService.injectReleases();

        verify(releaseRepository).saveAll(anySet());
        verify(releasePullRequestRepository, atLeast(0)).saveAll(anySet());
    }

    @Test
    public void doesNothing_whenNoValidReleases() throws Exception {
        when(gitHubGraphQLClient.getReleases()).thenReturn(Collections.emptySet());
        releaseInjectionService.injectReleases();
        verify(releaseRepository, never()).saveAll(anySet());
    }

    @Test
    public void deletesObsoleteReleases_andCleansUpVulnerabilities() throws Exception {
        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(dto2));
        when(branchInjectionService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(rel2);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel2));
        when(branchInjectionService.getBranchPullRequestsByBranches(anyList())).thenReturn(Collections.emptyMap());

        Release obsoleteRelease = new Release();
        obsoleteRelease.setId("obsolete-id");
        obsoleteRelease.setName("v1.0");
        when(releaseRepository.findAll()).thenReturn(List.of(obsoleteRelease, rel2));

        releaseInjectionService.injectReleases();

        verify(releaseVulnerabilityRepository).deleteAllByReleaseId("obsolete-id");
        verify(releasePullRequestRepository).deleteAllByReleaseId("obsolete-id");
        verify(releaseRepository)
                .deleteAll(argThat(
                        iterable -> iterable instanceof List && ((List<?>) iterable).contains(obsoleteRelease)));
    }

    @Test
    public void fallbackToMasterBranch_whenNoVersionBranchMatches() throws Exception {
        ReleaseDTO dto = new ReleaseDTO("id", "v9.9", "v9.9", new ReleaseTagCommitDTO(OffsetDateTime.now()));

        Release rel = new Release();
        rel.setTagName("v9.9");
        rel.setPublishedAt(dto.getReleaseDate());
        rel.setBranch(masterBranch);

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(dto));
        when(branchInjectionService.getAllBranches()).thenReturn(List.of(masterBranch, featureBranch, masterBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(rel);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel));
        when(branchInjectionService.getBranchPullRequestsByBranches(anyList())).thenReturn(Collections.emptyMap());

        releaseInjectionService.injectReleases();

        verify(releaseRepository).saveAll(anySet());
        verify(releasePullRequestRepository, never()).saveAll(anySet());
    }

    @Test
    public void nullBranch_ifNoMatchAndNoMaster() throws Exception {
        ReleaseDTO dto = new ReleaseDTO("id", "v999.999", "v999.9990", new ReleaseTagCommitDTO(OffsetDateTime.now()));

        Release rel = new Release();
        rel.setTagName("v999.999");
        rel.setPublishedAt(dto.getReleaseDate());
        rel.setBranch(null);

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(dto));
        when(branchInjectionService.getAllBranches()).thenReturn(List.of(masterBranch, featureBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(rel);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel));
        when(branchInjectionService.getBranchPullRequestsByBranches(anyList())).thenReturn(Collections.emptyMap());

        releaseInjectionService.injectReleases();

        verify(releaseRepository).saveAll(anySet());
    }

    @Test
    public void assignsPullRequestsToCorrectReleaseByTimeframe() throws Exception {
        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(dto1, dto2));
        when(branchInjectionService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(dto1, Release.class)).thenReturn(rel1);
        when(mapper.toEntity(dto2, Release.class)).thenReturn(rel2);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel1, rel2));
        when(branchInjectionService.getBranchPullRequestsByBranches(anyList()))
                .thenReturn(Map.of(masterBranch.getId(), Set.of(branchPR1)));

        releaseInjectionService.injectReleases();

        verify(releasePullRequestRepository, atLeastOnce()).saveAll(anySet());
    }

    @Test
    public void assignsNothing_whenNoMatchingBranches() throws Exception {
        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(dto1));
        when(branchInjectionService.getAllBranches()).thenReturn(Collections.emptyList());
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(rel1);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel1));

        releaseInjectionService.injectReleases();

        verify(releaseRepository).saveAll(anySet());
        verify(releasePullRequestRepository, never()).saveAll(anySet());
    }

    @Test
    public void handlesNullBranchNameGracefully() throws Exception {
        Release relWithNull = new Release();
        relWithNull.setTagName("vX.Y");
        relWithNull.setPublishedAt(OffsetDateTime.now());
        relWithNull.setBranch(noNameBranch);

        ReleaseDTO dto = new ReleaseDTO("id", "vX.Y", "vX.Y", new ReleaseTagCommitDTO(relWithNull.getPublishedAt()));

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(dto));
        when(branchInjectionService.getAllBranches()).thenReturn(List.of(noNameBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(relWithNull);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(relWithNull));
        when(branchInjectionService.getBranchPullRequestsByBranches(anyList()))
                .thenReturn(Map.of(noNameBranch.getId(), Set.of()));

        releaseInjectionService.injectReleases();

        verify(releaseRepository).saveAll(anySet());
    }

    @Test
    public void noPullRequestsAssigned_ifNoneInTimeWindow() throws Exception {
        pr1.setMergedAt(OffsetDateTime.now().plusYears(5));
        branchPR1 = new BranchPullRequest(masterBranch, pr1);

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(dto1, dto2));
        when(branchInjectionService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(dto1, Release.class)).thenReturn(rel1);
        when(mapper.toEntity(dto2, Release.class)).thenReturn(rel2);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel1, rel2));
        when(branchInjectionService.getBranchPullRequestsByBranches(anyList()))
                .thenReturn(Map.of(masterBranch.getId(), Set.of(branchPR1)));

        releaseInjectionService.injectReleases();

        verify(releasePullRequestRepository, never()).saveAll(anySet());
    }

    @Test
    public void masterBranchWithReleases_assignsPRsToMaster() throws Exception {
        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(dto1));
        when(branchInjectionService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(rel1);
        rel1.setBranch(masterBranch);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel1));
        when(branchInjectionService.getBranchPullRequestsByBranches(anyList()))
                .thenReturn(Map.of(masterBranch.getId(), Set.of(branchPR1)));

        releaseInjectionService.injectReleases();

        verify(releaseRepository).saveAll(anySet());
        verify(releasePullRequestRepository, atLeast(0)).saveAll(anySet());
    }

    @Test
    public void malformedTagName_fallsBackToMasterOrNull() throws Exception {
        Release relMalformed = new Release();
        relMalformed.setTagName("foo_bar");
        relMalformed.setPublishedAt(dtoMalformed.getReleaseDate());
        relMalformed.setBranch(masterBranch);

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(dtoMalformed));
        when(branchInjectionService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(relMalformed);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(relMalformed));
        when(branchInjectionService.getBranchPullRequestsByBranches(anyList())).thenReturn(Collections.emptyMap());

        releaseInjectionService.injectReleases();
        verify(releaseRepository).saveAll(anySet());
    }

    @Test
    public void throwsReleaseInjectionException_onGitHubClientException() throws GitHubGraphQLClientException {
        when(gitHubGraphQLClient.getReleases()).thenThrow(new GitHubGraphQLClientException("fail", null));
        assertThrows(ReleaseInjectionException.class, () -> releaseInjectionService.injectReleases());
    }

    @Test
    public void extractMajorMinor_variousCases() throws Exception {
        ReleaseDTO tagGood = new ReleaseDTO("id", "v3.5", "v3.5", new ReleaseTagCommitDTO(OffsetDateTime.now()));
        Release relGood = new Release();
        relGood.setTagName("v3.5");
        relGood.setPublishedAt(tagGood.getReleaseDate());
        relGood.setBranch(masterBranch);

        ReleaseDTO tagBad = new ReleaseDTO("id", "vX.Y", "vX.Y", new ReleaseTagCommitDTO(OffsetDateTime.now()));
        Release relBad = new Release();
        relBad.setTagName("vX.Y");
        relBad.setPublishedAt(tagBad.getReleaseDate());
        relBad.setBranch(masterBranch);

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(tagGood, tagBad));
        when(branchInjectionService.getAllBranches()).thenReturn(List.of(masterBranch, masterBranch, featureBranch));
        when(mapper.toEntity(tagGood, Release.class)).thenReturn(relGood);
        when(mapper.toEntity(tagBad, Release.class)).thenReturn(relBad);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(relGood, relBad));
        when(branchInjectionService.getBranchPullRequestsByBranches(anyList())).thenReturn(Collections.emptyMap());

        releaseInjectionService.injectReleases();
        verify(releaseRepository).saveAll(anySet());
    }

    @Test
    public void injectReleases_shouldAssignPRsToCorrectReleases_whenPRsFallInTimeWindows() throws Exception {
        // Arrange
        dto1 = new ReleaseDTO(
                "id1", "v1.0", "v1.0", new ReleaseTagCommitDTO(OffsetDateTime.parse("2025-08-01T10:00:00Z")));
        dto2 = new ReleaseDTO(
                "id2", "v1.1", "v1.1", new ReleaseTagCommitDTO(OffsetDateTime.parse("2025-08-10T10:00:00Z")));
        ReleaseDTO dto3 = new ReleaseDTO(
                "id3", "v1.2", "v1.2", new ReleaseTagCommitDTO(OffsetDateTime.parse("2025-08-20T10:00:00Z")));

        rel1.setId("r1");
        rel1.setName("v1.0");
        rel1.setPublishedAt(dto1.getReleaseDate());
        rel1.setBranch(masterBranch);

        rel2.setId("r2");
        rel2.setName("v1.1");
        rel2.setPublishedAt(dto2.getReleaseDate());
        rel2.setBranch(masterBranch);

        Release rel3 = new Release();
        rel3.setId("r3");
        rel3.setName("v1.2");
        rel3.setPublishedAt(dto3.getReleaseDate());
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
        when(branchInjectionService.getAllBranches()).thenReturn(List.of(masterBranch));
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
        when(branchInjectionService.getBranchPullRequestsByBranches(anyList()))
                .thenReturn(Map.of(masterBranch.getId(), Set.of(bpr1, bpr2)));

        ArgumentCaptor<Set<ReleasePullRequest>> captor = ArgumentCaptor.forClass(Set.class);

        releaseInjectionService.injectReleases();

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
        Release normalEarly = new Release();
        normalEarly.setName("v1.0");
        normalEarly.setPublishedAt(OffsetDateTime.parse("2025-08-10T10:00:00Z"));

        Release nightlyMiddle = new Release();
        nightlyMiddle.setName("A nightly build");
        nightlyMiddle.setPublishedAt(OffsetDateTime.parse("2025-08-15T10:00:00Z"));

        Release normalLate = new Release();
        normalLate.setName("v1.1");
        normalLate.setPublishedAt(OffsetDateTime.parse("2025-08-20T10:00:00Z"));

        Release nullNameRelease = new Release();
        nullNameRelease.setName(null);
        nullNameRelease.setPublishedAt(OffsetDateTime.parse("2025-08-01T10:00:00Z"));

        List<Release> releases = Arrays.asList(normalEarly, nightlyMiddle, normalLate, nullNameRelease);
        Collections.shuffle(releases);

        Comparator<Release> comparator = releaseInjectionService.getReleaseSortingComparator();
        releases.sort(comparator);

        assertEquals(nullNameRelease, releases.get(0));
        assertEquals(normalEarly, releases.get(1));
        assertEquals(normalLate, releases.get(2));
        assertEquals(nightlyMiddle, releases.get(3));
    }

    @Test
    public void isValidRelease_shouldFilterOutReleaseCandidate() throws Exception {
        ReleaseDTO rcRelease =
                new ReleaseDTO("id1", "v8.1.0-RC1", "v8.1.0-RC1", new ReleaseTagCommitDTO(OffsetDateTime.now()));
        ReleaseDTO validRelease =
                new ReleaseDTO("id2", "v8.1.0", "v8.1.0", new ReleaseTagCommitDTO(OffsetDateTime.now()));

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(rcRelease, validRelease));
        when(branchInjectionService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(validRelease, Release.class)).thenReturn(rel1);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel1));
        when(branchInjectionService.getBranchPullRequestsByBranches(anyList())).thenReturn(Collections.emptyMap());

        releaseInjectionService.injectReleases();

        ArgumentCaptor<Set<Release>> captor = ArgumentCaptor.forClass(Set.class);
        verify(releaseRepository).saveAll(captor.capture());
        Set<Release> savedReleases = captor.getValue();

        assertEquals(1, savedReleases.size());
        verify(mapper, times(1)).toEntity(validRelease, Release.class);
        verify(mapper, never()).toEntity(rcRelease, Release.class);
    }

    @Test
    public void isValidRelease_shouldFilterOutBetaRelease() throws Exception {
        ReleaseDTO betaRelease =
                new ReleaseDTO("id1", "v7.0-B2", "v7.0-B2", new ReleaseTagCommitDTO(OffsetDateTime.now()));
        ReleaseDTO validRelease =
                new ReleaseDTO("id2", "v7.0.0", "v7.0.0", new ReleaseTagCommitDTO(OffsetDateTime.now()));

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(betaRelease, validRelease));
        when(branchInjectionService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(validRelease, Release.class)).thenReturn(rel1);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel1));
        when(branchInjectionService.getBranchPullRequestsByBranches(anyList())).thenReturn(Collections.emptyMap());

        releaseInjectionService.injectReleases();

        ArgumentCaptor<Set<Release>> captor = ArgumentCaptor.forClass(Set.class);
        verify(releaseRepository).saveAll(captor.capture());
        Set<Release> savedReleases = captor.getValue();

        assertEquals(1, savedReleases.size());
        verify(mapper, times(1)).toEntity(validRelease, Release.class);
        verify(mapper, never()).toEntity(betaRelease, Release.class);
    }

    @Test
    public void isValidRelease_shouldFilterOutMultipleInvalidReleases() throws Exception {
        ReleaseDTO rc1 = new ReleaseDTO("id1", "v7.8-RC1", "v7.8-RC1", new ReleaseTagCommitDTO(OffsetDateTime.now()));
        ReleaseDTO rc2 = new ReleaseDTO("id2", "v7.8-RC2", "v7.8-RC2", new ReleaseTagCommitDTO(OffsetDateTime.now()));
        ReleaseDTO beta = new ReleaseDTO("id3", "v7.0-B3", "v7.0-B3", new ReleaseTagCommitDTO(OffsetDateTime.now()));
        ReleaseDTO validRelease =
                new ReleaseDTO("id4", "v7.8.0", "v7.8.0", new ReleaseTagCommitDTO(OffsetDateTime.now()));

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(rc1, rc2, beta, validRelease));
        when(branchInjectionService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(validRelease, Release.class)).thenReturn(rel1);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel1));
        when(branchInjectionService.getBranchPullRequestsByBranches(anyList())).thenReturn(Collections.emptyMap());

        releaseInjectionService.injectReleases();

        ArgumentCaptor<Set<Release>> captor = ArgumentCaptor.forClass(Set.class);
        verify(releaseRepository).saveAll(captor.capture());
        Set<Release> savedReleases = captor.getValue();

        assertEquals(1, savedReleases.size());
        verify(mapper, times(1)).toEntity(validRelease, Release.class);
        verify(mapper, never()).toEntity(rc1, Release.class);
        verify(mapper, never()).toEntity(rc2, Release.class);
        verify(mapper, never()).toEntity(beta, Release.class);
    }

    @Test
    public void isValidRelease_shouldHandleNullReleaseName() throws Exception {
        ReleaseDTO nullNameRelease = new ReleaseDTO("id1", null, null, new ReleaseTagCommitDTO(OffsetDateTime.now()));

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(nullNameRelease));
        when(branchInjectionService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(branchInjectionService.getBranchPullRequestsByBranches(anyList())).thenReturn(Collections.emptyMap());

        releaseInjectionService.injectReleases();

        verify(releaseRepository, never()).saveAll(anySet());
        verify(mapper, never()).toEntity(any(ReleaseDTO.class), eq(Release.class));
    }

    @Test
    public void isValidRelease_shouldAllowCaseInsensitiveMatching() throws Exception {
        ReleaseDTO rcLowercase =
                new ReleaseDTO("id1", "v7.5-rc2", "v7.5-rc2", new ReleaseTagCommitDTO(OffsetDateTime.now()));
        ReleaseDTO betaUppercase =
                new ReleaseDTO("id2", "v7.6-B1", "v7.6-B1", new ReleaseTagCommitDTO(OffsetDateTime.now()));
        ReleaseDTO validRelease =
                new ReleaseDTO("id3", "v7.6.0", "v7.6.0", new ReleaseTagCommitDTO(OffsetDateTime.now()));

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(rcLowercase, betaUppercase, validRelease));
        when(branchInjectionService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(validRelease, Release.class)).thenReturn(rel1);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(rel1));
        when(branchInjectionService.getBranchPullRequestsByBranches(anyList())).thenReturn(Collections.emptyMap());

        releaseInjectionService.injectReleases();

        ArgumentCaptor<Set<Release>> captor = ArgumentCaptor.forClass(Set.class);
        verify(releaseRepository).saveAll(captor.capture());
        Set<Release> savedReleases = captor.getValue();

        assertEquals(1, savedReleases.size());
        verify(mapper, times(1)).toEntity(validRelease, Release.class);
        verify(mapper, never()).toEntity(rcLowercase, Release.class);
        verify(mapper, never()).toEntity(betaUppercase, Release.class);
    }

    @Test
    public void saveAllReleases_shouldPreserveExistingLastScannedValue() throws Exception {
        OffsetDateTime existingLastScanned = OffsetDateTime.parse("2025-06-15T10:00:00Z");

        Release existingRelease = new Release();
        existingRelease.setId("id1");
        existingRelease.setTagName("v1.0");
        existingRelease.setLastScanned(existingLastScanned);

        Release newReleaseFromGitHub = new Release();
        newReleaseFromGitHub.setId("id1");
        newReleaseFromGitHub.setTagName("v1.0");
        newReleaseFromGitHub.setPublishedAt(dto1.getReleaseDate());
        newReleaseFromGitHub.setBranch(masterBranch);
        newReleaseFromGitHub.setLastScanned(null);

        when(gitHubGraphQLClient.getReleases()).thenReturn(Set.of(dto1));
        when(branchInjectionService.getAllBranches()).thenReturn(List.of(masterBranch));
        when(mapper.toEntity(any(ReleaseDTO.class), eq(Release.class))).thenReturn(newReleaseFromGitHub);
        when(releaseRepository.findAll()).thenReturn(List.of(existingRelease));
        when(releaseRepository.saveAll(anySet())).thenAnswer(invocation -> {
            Set<Release> releases = invocation.getArgument(0);
            return new ArrayList<>(releases);
        });
        when(branchInjectionService.getBranchPullRequestsByBranches(anyList())).thenReturn(Collections.emptyMap());

        releaseInjectionService.injectReleases();

        ArgumentCaptor<Set<Release>> captor = ArgumentCaptor.forClass(Set.class);
        verify(releaseRepository).saveAll(captor.capture());
        Set<Release> savedReleases = captor.getValue();

        assertEquals(1, savedReleases.size());
        Release savedRelease = savedReleases.iterator().next();
        assertEquals(existingLastScanned, savedRelease.getLastScanned());
    }
}
