package org.frankframework.webapp.label;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.frankframework.shared.entity.Release;
import org.frankframework.webapp.common.configuration.properties.GitHubProperties;
import org.frankframework.webapp.common.entityconnection.issuelabel.IssueLabel;
import org.frankframework.webapp.common.entityconnection.issuelabel.IssueLabelRepository;
import org.frankframework.webapp.common.mapper.Mapper;
import org.frankframework.webapp.common.mapper.MappingException;
import org.frankframework.webapp.github.GitHubClient;
import org.frankframework.webapp.github.GitHubClientException;
import org.frankframework.webapp.github.GitHubRepositoryStatisticsDTO;
import org.frankframework.webapp.github.GitHubRepositoryStatisticsService;
import org.frankframework.webapp.release.ReleaseNotFoundException;
import org.frankframework.webapp.release.ReleaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
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

    @Captor
    private ArgumentCaptor<LinkedHashSet<Label>> labelSetCaptor;

    private LabelService labelService;

    @BeforeEach
    public void setUp() {
        List<String> includedColors = List.of("D73A4A", "B60205", "007BFF", "1D76DB", "123456");

        GitHubProperties gitHubProperties = mock(GitHubProperties.class);
        when(gitHubProperties.getIncludedLabels()).thenReturn(includedColors);

        labelService = new LabelService(
                statisticsService,
                gitHubClient,
                mapper,
                labelRepository,
                issueLabelRepository,
                gitHubProperties,
                releaseService);
    }

    @Test
    public void shouldSkipIfLabelCountsAreEqual() throws LabelInjectionException, GitHubClientException {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
        when(statisticsDTO.getGitHubLabelCount()).thenReturn(5);
        when(labelRepository.count()).thenReturn(5L);

        labelService.injectLabels();

        verify(gitHubClient, never()).getLabels();
        verify(labelRepository, never()).saveAll(anySet());
    }

    @Test
    public void shouldSaveAllLabelsWhenCountsDiffer() throws Exception {
        LabelDTO dto1 = new LabelDTO("l1", "bug", "Of type bug", "D73A4A");
        LabelDTO dto2 = new LabelDTO("l2", "feature", "A new feature", "007BFF");
        Set<LabelDTO> dtos = Set.of(dto1, dto2);
        Set<Label> entities = Set.of(createLabel("l1", "bug", "Of type bug", "D73A4A"));

        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
        when(statisticsDTO.getGitHubLabelCount()).thenReturn(10);
        when(labelRepository.count()).thenReturn(1L);
        when(gitHubClient.getLabels()).thenReturn(dtos);
        when(mapper.toEntity(dtos, Label.class)).thenReturn(entities);

        labelService.injectLabels();

        verify(labelRepository).saveAll(entities);
    }

    @Test
    public void shouldThrowLabelInjectionException_whenClientFails() throws GitHubClientException {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
        when(statisticsDTO.getGitHubLabelCount()).thenReturn(4);
        when(labelRepository.count()).thenReturn(1L);
        when(gitHubClient.getLabels()).thenThrow(new GitHubClientException("API fetch failed", null));

        assertThrows(LabelInjectionException.class, () -> labelService.injectLabels());
    }

    @Test
    public void shouldThrowLabelInjectionException_whenMappingFails() throws Exception {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
        when(statisticsDTO.getGitHubLabelCount()).thenReturn(10);
        when(labelRepository.count()).thenReturn(0L);
        when(gitHubClient.getLabels()).thenReturn(Collections.emptySet());
        when(mapper.toEntity(anySet(), eq(Label.class))).thenThrow(new MappingException("Mapping failed", null));

        assertThrows(LabelInjectionException.class, () -> labelService.injectLabels());
    }

    @Test
    public void shouldCorrectlyFilterAndSortLabelsByPopularity() throws Exception {
        Release release = new Release();
        release.setId("r1");
        when(releaseService.checkIfReleaseExists("r1")).thenReturn(release);
        Label labelBug = createLabel("l1", "bug", "A bug", "D73A4A");
        Label labelFeature = createLabel("l2", "feature", "Popular feature", "007BFF");
        Label labelWontfix = createLabel("l3", "wontfix", "Not included", "EEEEEE");
        Label labelDocs = createLabel("l4", "docs", "Documentation", "1D76DB");

        List<Label> releaseLabels = Stream.of(
                        Collections.nCopies(5, labelFeature),
                        Collections.nCopies(3, labelDocs),
                        Collections.nCopies(2, labelBug),
                        Collections.nCopies(10, labelWontfix))
                .flatMap(List::stream)
                .collect(Collectors.toList());
        when(labelRepository.findLabelsByReleaseId("r1")).thenReturn(releaseLabels);

        labelService.getHighlightsByReleaseId("r1");

        verify(mapper).toDTO(labelSetCaptor.capture(), eq(LabelResponse.class));
        List<Label> highlights = new ArrayList<>(labelSetCaptor.getValue());

        assertEquals(3, highlights.size(), "Should contain 3 labels after filtering.");
        assertEquals("l2", highlights.get(0).getId(), "Most popular included label (feature) should be first.");
        assertEquals("l4", highlights.get(1).getId(), "Second most popular included label (docs) should be second.");
        assertEquals("l1", highlights.get(2).getId(), "Least popular included label (bug) should be third.");
    }

    @Test
    public void shouldHandleCaseInsensitiveColorsForInclusion() throws Exception {
        Release release = new Release();
        release.setId("r2");
        when(releaseService.checkIfReleaseExists("r2")).thenReturn(release);
        Label includedLower = createLabel("p1", "bugfix", "", "d73a4a");
        Label notIncluded = createLabel("i1", "duplicate", "", "fBcA04");
        Label includedValid = createLabel("v1", "Valid", "", "123456");

        List<Label> labels = Stream.of(
                        Collections.nCopies(2, includedValid),
                        Collections.nCopies(5, notIncluded),
                        Collections.nCopies(1, includedLower))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        when(labelRepository.findLabelsByReleaseId("r2")).thenReturn(labels);

        labelService.getHighlightsByReleaseId("r2");

        verify(mapper).toDTO(labelSetCaptor.capture(), eq(LabelResponse.class));
        List<Label> highlights = new ArrayList<>(labelSetCaptor.getValue());

        assertEquals(2, highlights.size(), "Should contain 2 labels after filtering.");
        assertEquals("v1", highlights.get(0).getId(), "The more popular included label should be first.");
        assertEquals("p1", highlights.get(1).getId(), "Label with lowercase color should be included and second.");
    }

    @Test
    public void shouldLimitToMaxHighlightedLabels() throws Exception {
        Release release = new Release();
        release.setId("max");
        when(releaseService.checkIfReleaseExists("max")).thenReturn(release);
        List<Label> manyLabels = IntStream.range(0, 20)
                .mapToObj(i -> createLabel("l" + i, "n" + i, "d" + i, "007BFF"))
                .collect(Collectors.toList());
        when(labelRepository.findLabelsByReleaseId("max")).thenReturn(manyLabels);

        labelService.getHighlightsByReleaseId("max");

        verify(mapper).toDTO(labelSetCaptor.capture(), eq(LabelResponse.class));
        assertEquals(15, labelSetCaptor.getValue().size(), "Result should be limited to 15 labels.");
    }

    @Test
    public void shouldReturnEmptySet_whenReleaseHasNoLabels() throws Exception {
        Release release = new Release();
        release.setId("relX");
        when(releaseService.checkIfReleaseExists("relX")).thenReturn(release);
        when(labelRepository.findLabelsByReleaseId("relX")).thenReturn(Collections.emptyList());

        Set<LabelResponse> result = labelService.getHighlightsByReleaseId("relX");

        assertTrue(result.isEmpty());
        verify(mapper, never()).toDTO(anySet(), eq(LabelResponse.class));
    }

    @Test
    public void shouldReturnEmptySet_ifNoLabelsAreIncluded() throws Exception {
        Release release = new Release();
        release.setId("r_not_included");
        when(releaseService.checkIfReleaseExists("r_not_included")).thenReturn(release);
        Label notIncluded1 = createLabel("i1", "ignored1", "desc", "EEEEEE");
        Label notIncluded2 = createLabel("i2", "ignored2", "desc", "FBCA04");

        when(labelRepository.findLabelsByReleaseId("r_not_included")).thenReturn(List.of(notIncluded1, notIncluded2));
        when(mapper.toDTO(anySet(), eq(LabelResponse.class))).thenReturn(Collections.emptySet());

        Set<LabelResponse> result = labelService.getHighlightsByReleaseId("r_not_included");
        assertTrue(result.isEmpty());
    }

    @Test
    public void shouldThrowReleaseNotFoundException() throws ReleaseNotFoundException {
        when(releaseService.checkIfReleaseExists("notfound"))
                .thenThrow(new ReleaseNotFoundException("Release not found", null));
        assertThrows(ReleaseNotFoundException.class, () -> labelService.getHighlightsByReleaseId("notfound"));
    }

    @Test
    public void shouldReturnAssociatedLabels() {
        Label label1 = createLabel("l1", "feature", "desc", "blue");
        Label label2 = createLabel("l2", "bug", "desc", "red");
        IssueLabel il1 = new IssueLabel();
        il1.setLabel(label1);
        IssueLabel il2 = new IssueLabel();
        il2.setLabel(label2);
        when(issueLabelRepository.findAllByIssue_Id("i1")).thenReturn(Set.of(il1, il2));

        Set<Label> result = labelService.getLabelsByIssueId("i1");

        assertEquals(2, result.size());
        assertTrue(result.contains(label1));
        assertTrue(result.contains(label2));
    }

    @Test
    public void shouldReturnEmptySet_whenNoLabelsFound() {
        when(issueLabelRepository.findAllByIssue_Id("i99")).thenReturn(Collections.emptySet());
        Set<Label> result = labelService.getLabelsByIssueId("i99");
        assertTrue(result.isEmpty());
    }

    private Label createLabel(String id, String name, String description, String color) {
        Label label = new Label();
        label.setId(id);
        label.setName(name);
        label.setDescription(description);
        label.setColor(color);
        return label;
    }
}
