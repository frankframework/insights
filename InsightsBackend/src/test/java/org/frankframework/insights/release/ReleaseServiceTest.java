package org.frankframework.insights.release;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.*;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequest;
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
    private GitHubRepositoryStatisticsService statisticsService;

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
    private GitHubRepositoryStatisticsDTO mockStatsDTO;

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

        mockStatsDTO = mock(GitHubRepositoryStatisticsDTO.class);
    }

    @Test
    public void skips_whenDatabaseUpToDate() throws Exception {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(mockStatsDTO);
        when(mockStatsDTO.getGitHubReleaseCount()).thenReturn(2);
        when(releaseRepository.count()).thenReturn(2L);
        releaseService.injectReleases();
        verify(gitHubClient, never()).getReleases();
        verify(releaseRepository, never()).saveAll(anySet());
    }

    @Test
    public void injects_whenDatabaseEmpty() throws Exception {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(mockStatsDTO);
        when(mockStatsDTO.getGitHubReleaseCount()).thenReturn(1);
        when(releaseRepository.count()).thenReturn(0L);
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
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(mockStatsDTO);
        when(mockStatsDTO.getGitHubReleaseCount()).thenReturn(1);
        when(releaseRepository.count()).thenReturn(0L);
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

        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(mockStatsDTO);
        when(mockStatsDTO.getGitHubReleaseCount()).thenReturn(1);
        when(releaseRepository.count()).thenReturn(0L);
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

        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(mockStatsDTO);
        when(mockStatsDTO.getGitHubReleaseCount()).thenReturn(1);
        when(releaseRepository.count()).thenReturn(0L);
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
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(mockStatsDTO);
        when(mockStatsDTO.getGitHubReleaseCount()).thenReturn(2);
        when(releaseRepository.count()).thenReturn(0L);
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
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(mockStatsDTO);
        when(mockStatsDTO.getGitHubReleaseCount()).thenReturn(1);
        when(releaseRepository.count()).thenReturn(0L);
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

        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(mockStatsDTO);
        when(mockStatsDTO.getGitHubReleaseCount()).thenReturn(1);
        when(releaseRepository.count()).thenReturn(0L);
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

        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(mockStatsDTO);
        when(mockStatsDTO.getGitHubReleaseCount()).thenReturn(2);
        when(releaseRepository.count()).thenReturn(0L);
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
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(mockStatsDTO);
        when(mockStatsDTO.getGitHubReleaseCount()).thenReturn(1);
        when(releaseRepository.count()).thenReturn(0L);
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

        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(mockStatsDTO);
        when(mockStatsDTO.getGitHubReleaseCount()).thenReturn(1);
        when(releaseRepository.count()).thenReturn(0L);
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
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(mockStatsDTO);
        when(mockStatsDTO.getGitHubReleaseCount()).thenReturn(1);
        when(releaseRepository.count()).thenReturn(0L);
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

        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(mockStatsDTO);
        when(mockStatsDTO.getGitHubReleaseCount()).thenReturn(2);
        when(releaseRepository.count()).thenReturn(0L);
        when(gitHubClient.getReleases()).thenReturn(Set.of(tagGood, tagBad));
        when(branchService.getAllBranches()).thenReturn(List.of(masterBranch, masterBranch, featureBranch));
        when(mapper.toEntity(eq(tagGood), eq(Release.class))).thenReturn(relGood);
        when(mapper.toEntity(eq(tagBad), eq(Release.class))).thenReturn(relBad);
        when(releaseRepository.saveAll(anySet())).thenReturn(List.of(relGood, relBad));
        when(branchService.getBranchPullRequestsByBranches(anyList())).thenReturn(Collections.emptyMap());

        releaseService.injectReleases();
        verify(releaseRepository).saveAll(anySet());
    }
}
