package org.frankframework.insights.label;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabel;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabelRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.properties.GitHubProperties;
import org.frankframework.insights.release.Release;
import org.frankframework.insights.release.ReleaseNotFoundException;
import org.frankframework.insights.release.ReleaseQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LabelQueryServiceTest {

    @Mock
    private Mapper mapper;

    @Mock
    private LabelRepository labelRepository;

    @Mock
    private IssueLabelRepository issueLabelRepository;

    @Mock
    private ReleaseQueryService releaseQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private GitHubProperties gitHubProperties;

    @Captor
    private ArgumentCaptor<LinkedHashSet<Label>> labelSetCaptor;

    private LabelQueryService labelQueryService;

    @BeforeEach
    public void setUp() {
        List<String> includedColors = List.of("D73A4A", "B60205", "007BFF", "1D76DB", "123456");

        when(gitHubProperties.getGraphql().getIncludedLabels()).thenReturn(includedColors);

        labelQueryService = new LabelQueryService(
                mapper, labelRepository, issueLabelRepository, gitHubProperties, releaseQueryService);
    }

    @Test
    public void shouldCorrectlyFilterAndSortLabelsByPopularity() throws Exception {
        Release release = new Release();
        release.setId("r1");
        when(releaseQueryService.checkIfReleaseExists("r1")).thenReturn(release);
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
                .toList();

        when(labelRepository.findLabelsByReleaseId("r1")).thenReturn(releaseLabels);

        labelQueryService.getHighlightsByReleaseId("r1");

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
        when(releaseQueryService.checkIfReleaseExists("r2")).thenReturn(release);
        Label includedLower = createLabel("p1", "bugfix", "", "d73a4a");
        Label notIncluded = createLabel("i1", "duplicate", "", "fBcA04");
        Label includedValid = createLabel("v1", "Valid", "", "123456");

        List<Label> labels = Stream.of(
                        Collections.nCopies(2, includedValid),
                        Collections.nCopies(5, notIncluded),
                        Collections.nCopies(1, includedLower))
                .flatMap(List::stream)
                .toList();

        when(labelRepository.findLabelsByReleaseId("r2")).thenReturn(labels);

        labelQueryService.getHighlightsByReleaseId("r2");

        verify(mapper).toDTO(labelSetCaptor.capture(), eq(LabelResponse.class));
        List<Label> highlights = new ArrayList<>(labelSetCaptor.getValue());

        assertEquals(2, highlights.size(), "Should contain 2 labels after filtering.");
        assertEquals("v1", highlights.get(0).getId(), "The more popular included label should be first.");
        assertEquals("p1", highlights.get(1).getId(), "Label with lowercase color should be included and second.");
    }

    @Test
    public void shouldReturnEmptySet_whenReleaseHasNoLabels() throws Exception {
        Release release = new Release();
        release.setId("relX");
        when(releaseQueryService.checkIfReleaseExists("relX")).thenReturn(release);
        when(labelRepository.findLabelsByReleaseId("relX")).thenReturn(Collections.emptyList());

        Set<LabelResponse> result = labelQueryService.getHighlightsByReleaseId("relX");

        assertTrue(result.isEmpty());
        verify(mapper, never()).toDTO(anySet(), eq(LabelResponse.class));
    }

    @Test
    public void shouldReturnEmptySet_ifNoLabelsAreIncluded() throws Exception {
        Release release = new Release();
        release.setId("r_not_included");
        when(releaseQueryService.checkIfReleaseExists("r_not_included")).thenReturn(release);
        Label notIncluded1 = createLabel("i1", "ignored1", "desc", "EEEEEE");
        Label notIncluded2 = createLabel("i2", "ignored2", "desc", "FBCA04");

        when(labelRepository.findLabelsByReleaseId("r_not_included")).thenReturn(List.of(notIncluded1, notIncluded2));
        when(mapper.toDTO(anySet(), eq(LabelResponse.class))).thenReturn(Collections.emptySet());

        Set<LabelResponse> result = labelQueryService.getHighlightsByReleaseId("r_not_included");
        assertTrue(result.isEmpty());
    }

    @Test
    public void shouldThrowReleaseNotFoundException() throws ReleaseNotFoundException {
        when(releaseQueryService.checkIfReleaseExists("notfound"))
                .thenThrow(new ReleaseNotFoundException("Release not found", null));
        assertThrows(ReleaseNotFoundException.class, () -> labelQueryService.getHighlightsByReleaseId("notfound"));
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

        Set<Label> result = labelQueryService.getLabelsByIssueId("i1");

        assertEquals(2, result.size());
        assertTrue(result.contains(label1));
        assertTrue(result.contains(label2));
    }

    @Test
    public void shouldReturnEmptySet_whenNoLabelsFound() {
        when(issueLabelRepository.findAllByIssue_Id("i99")).thenReturn(Collections.emptySet());
        Set<Label> result = labelQueryService.getLabelsByIssueId("i99");
        assertTrue(result.isEmpty());
    }

    @Test
    public void shouldReturnTrueForIncludedLabelColor() {
        Label label = createLabel("l1", "bug", "desc", "D73A4A");
        assertTrue(labelQueryService.isLabelIncluded(label));
    }

    @Test
    public void shouldReturnTrueForIncludedLabelColorCaseInsensitive() {
        Label label = createLabel("l1", "bug", "desc", "d73a4a");
        assertTrue(labelQueryService.isLabelIncluded(label));
    }

    @Test
    public void shouldReturnFalseForNotIncludedLabelColor() {
        Label label = createLabel("l1", "wontfix", "desc", "EEEEEE");
        assertFalse(labelQueryService.isLabelIncluded(label));
    }

    @Test
    public void shouldReturnFalseForNullLabel() {
        assertFalse(labelQueryService.isLabelIncluded(null));
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
