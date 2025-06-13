package org.frankframework.insights.label;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.frankframework.insights.common.configuration.properties.GitHubProperties;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabel;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabelRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.mapper.MappingException;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubClientException;
import org.frankframework.insights.github.GitHubRepositoryStatisticsDTO;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.frankframework.insights.release.Release;
import org.frankframework.insights.release.ReleaseNotFoundException;
import org.frankframework.insights.release.ReleaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LabelServiceTest {

    @Mock
    private GitHubRepositoryStatisticsService statisticsService;

    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private Mapper mapper;

    @Mock
    private LabelRepository labelRepository;

    @Mock
    private IssueLabelRepository issueLabelRepository;

    @Mock
    private ReleaseService releaseService;

    @Mock
    private GitHubRepositoryStatisticsDTO statisticsDTO;

    private LabelService labelService;

    private Label labelBug;
    private Label labelFeature;
    private Label labelChore;
    private LabelDTO labelDTO1, labelDTO2;

    @BeforeEach
    public void setUp() {
        List<String> priorityColors = List.of("red");
        List<String> ignoredColors = List.of("yellow");

        GitHubProperties gitHubProperties = mock(GitHubProperties.class);
        when(gitHubProperties.getPriorityLabels()).thenReturn(priorityColors);
        when(gitHubProperties.getIgnoredLabels()).thenReturn(ignoredColors);

        labelService = new LabelService(
                statisticsService,
                gitHubClient,
                mapper,
                labelRepository,
                issueLabelRepository,
                gitHubProperties,
                releaseService);

        labelBug = new Label();
        labelBug.setId("l1");
        labelBug.setName("bug");
        labelBug.setDescription("Of type bug");
        labelBug.setColor("red");

        labelFeature = new Label();
        labelFeature.setId("l2");
        labelFeature.setName("feature");
        labelFeature.setDescription("Feature");
        labelFeature.setColor("blue");

        labelChore = new Label();
        labelChore.setId("l3");
        labelChore.setName("chore");
        labelChore.setDescription("Chore");
        labelChore.setColor("yellow");

        labelDTO1 = new LabelDTO("l1", "bug", "Of type bug", "red");
        labelDTO2 = new LabelDTO("l2", "feature", "Feature", "blue");
    }

    @Test
    public void injectLabels_shouldSkipIfCountsEqual() throws GitHubClientException, LabelInjectionException {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
        when(statisticsDTO.getGitHubLabelCount()).thenReturn(5);
        when(labelRepository.count()).thenReturn(5L);

        labelService.injectLabels();

        verify(gitHubClient, never()).getLabels();
        verify(labelRepository, never()).saveAll(anySet());
    }

    @Test
    public void injectLabels_shouldSaveAllLabels()
            throws GitHubClientException, MappingException, LabelInjectionException {
        Set<LabelDTO> dtos = Set.of(labelDTO1, labelDTO2);
        Set<Label> entities = Set.of(labelBug, labelFeature);
        List<Label> saved = List.of(labelBug, labelFeature);

        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
        when(statisticsDTO.getGitHubLabelCount()).thenReturn(3);
        when(labelRepository.count()).thenReturn(2L);
        when(gitHubClient.getLabels()).thenReturn(dtos);
        when(mapper.toEntity(dtos, Label.class)).thenReturn(entities);
        when(labelRepository.saveAll(entities)).thenReturn(saved);

        labelService.injectLabels();

        verify(labelRepository).saveAll(entities);
    }

    @Test
    public void injectLabels_shouldThrowOnException() throws GitHubClientException {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
        when(statisticsDTO.getGitHubLabelCount()).thenReturn(4);
        when(labelRepository.count()).thenReturn(1L);
        when(gitHubClient.getLabels()).thenThrow(new GitHubClientException("fail", null));

        assertThrows(LabelInjectionException.class, () -> labelService.injectLabels());
    }

    @Test
    public void getHighlightsByReleaseId_shouldReturnOnlyPriorityLabels()
            throws ReleaseNotFoundException, MappingException {
        Release release = new Release();
        release.setId("r1");
        when(releaseService.checkIfReleaseExists("r1")).thenReturn(release);

        List<Label> labels = List.of(labelBug, labelFeature);
        when(labelRepository.findLabelsByReleaseId("r1")).thenReturn(labels);

        Set<LabelResponse> responses = Set.of(new LabelResponse("l1", "bug", "Of type bug", "red"));
        when(mapper.toDTO(anySet(), eq(LabelResponse.class))).thenReturn(responses);

        Set<LabelResponse> result = labelService.getHighlightsByReleaseId("r1");

        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(r -> "red".equals(r.color())));
        verify(mapper).toDTO(anySet(), eq(LabelResponse.class));
    }

    @Test
    public void getHighlightsByReleaseId_shouldReturnEmptyIfNoPriorityLabels()
            throws ReleaseNotFoundException, MappingException {
        Release release = new Release();
        release.setId("r2");
        when(releaseService.checkIfReleaseExists("r2")).thenReturn(release);

        List<Label> labels = List.of(labelFeature, labelChore);
        when(labelRepository.findLabelsByReleaseId("r2")).thenReturn(labels);

        when(mapper.toDTO(anySet(), eq(LabelResponse.class))).thenReturn(Collections.emptySet());

        Set<LabelResponse> result = labelService.getHighlightsByReleaseId("r2");
        assertTrue(result.isEmpty());
    }

    @Test
    public void getHighlightsByReleaseId_shouldReturnEmptyIfNoLabels()
            throws ReleaseNotFoundException, MappingException {
        Release release = new Release();
        release.setId("relX");
        when(releaseService.checkIfReleaseExists("relX")).thenReturn(release);

        when(labelRepository.findLabelsByReleaseId("relX")).thenReturn(Collections.emptyList());
        when(mapper.toDTO(new LinkedHashSet<>(), LabelResponse.class)).thenReturn(Collections.emptySet());

        Set<LabelResponse> result = labelService.getHighlightsByReleaseId("relX");
        assertTrue(result.isEmpty());
    }

    @Test
    public void getHighlightsByReleaseId_shouldThrowIfReleaseNotFound() throws ReleaseNotFoundException {
        when(releaseService.checkIfReleaseExists("notfound"))
                .thenThrow(new ReleaseNotFoundException("Release not found", null));
        assertThrows(ReleaseNotFoundException.class, () -> labelService.getHighlightsByReleaseId("notfound"));
    }

    @Test
    public void getHighlightsByReleaseId_shouldThrowIfMappingFails() throws Exception {
        Release release = new Release();
        release.setId("failMap");
        when(releaseService.checkIfReleaseExists("failMap")).thenReturn(release);

        List<Label> labels = List.of(labelBug);
        when(labelRepository.findLabelsByReleaseId("failMap")).thenReturn(labels);
        when(mapper.toDTO(anySet(), eq(LabelResponse.class))).thenThrow(new MappingException("Mapping failed", null));

        assertThrows(MappingException.class, () -> labelService.getHighlightsByReleaseId("failMap"));
    }

    @Test
    public void getLabelsByIssueId_shouldReturnLabels() {
        IssueLabel il1 = new IssueLabel();
        il1.setLabel(labelFeature);

        IssueLabel il2 = new IssueLabel();
        il2.setLabel(labelBug);

        when(issueLabelRepository.findAllByIssue_Id("i1")).thenReturn(Set.of(il1, il2));
        Set<Label> result = labelService.getLabelsByIssueId("i1");
        assertEquals(2, result.size());
        assertTrue(result.contains(labelBug));
        assertTrue(result.contains(labelFeature));
    }

    @Test
    public void getLabelsByIssueId_shouldReturnEmptyIfNoLabels() {
        when(issueLabelRepository.findAllByIssue_Id("i99")).thenReturn(Set.of());
        Set<Label> result = labelService.getLabelsByIssueId("i99");
        assertTrue(result.isEmpty());
    }

    @Test
    public void saveLabels_shouldLogInfoAndSave()
            throws GitHubClientException, MappingException, LabelInjectionException {
        Set<Label> labels = Set.of(labelBug, labelFeature);
        when(labelRepository.saveAll(labels)).thenReturn(List.of(labelBug, labelFeature));

        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
        when(statisticsDTO.getGitHubLabelCount()).thenReturn(1);
        when(labelRepository.count()).thenReturn(0L);
        when(gitHubClient.getLabels()).thenReturn(Set.of(labelDTO1, labelDTO2));
        when(mapper.toEntity(anySet(), eq(Label.class))).thenReturn(labels);

        labelService.injectLabels();

        verify(labelRepository).saveAll(labels);
    }

    @Test
    public void getHighlightsByReleaseId_shouldIgnoreLabelsInIgnoredColors()
            throws ReleaseNotFoundException, MappingException {
        Release release = new Release();
        release.setId("r3");
        when(releaseService.checkIfReleaseExists("r3")).thenReturn(release);

        List<Label> labels = List.of(labelChore);
        when(labelRepository.findLabelsByReleaseId("r3")).thenReturn(labels);
        when(mapper.toDTO(new LinkedHashSet<>(), LabelResponse.class)).thenReturn(Collections.emptySet());

        Set<LabelResponse> result = labelService.getHighlightsByReleaseId("r3");
        assertTrue(result.isEmpty());
    }

    @Test
    public void getHighlightsByReleaseId_shouldLimitToMaxHighlightedLabels()
            throws ReleaseNotFoundException, MappingException {
        Release release = new Release();
        release.setId("max");
        when(releaseService.checkIfReleaseExists("max")).thenReturn(release);

        List<Label> manyPriorityLabels = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Label l = new Label();
            l.setId("l" + i);
            l.setName("n" + i);
            l.setDescription("desc" + i);
            l.setColor("red");
            manyPriorityLabels.add(l);
        }
        when(labelRepository.findLabelsByReleaseId("max")).thenReturn(manyPriorityLabels);

        Set<Label> expectedFiltered = new LinkedHashSet<>(manyPriorityLabels.subList(0, 15));
        Set<LabelResponse> responses = new HashSet<>();
        for (Label l : manyPriorityLabels.subList(0, 15)) {
            responses.add(new LabelResponse(l.getId(), l.getName(), l.getDescription(), l.getColor()));
        }
        when(mapper.toDTO(expectedFiltered, LabelResponse.class)).thenReturn(responses);

        Set<LabelResponse> result = labelService.getHighlightsByReleaseId("max");
        assertEquals(15, result.size());
    }
}
