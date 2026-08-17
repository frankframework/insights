package org.frankframework.insights.label;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.mapper.MappingException;
import org.frankframework.insights.github.graphql.GitHubGraphQLClient;
import org.frankframework.insights.github.graphql.GitHubGraphQLClientException;
import org.frankframework.insights.github.graphql.GitHubRepositoryStatisticsDTO;
import org.frankframework.insights.github.graphql.GitHubRepositoryStatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LabelInjectionServiceTest {

    @Mock
    private GitHubRepositoryStatisticsService statisticsService;

    @Mock
    private GitHubGraphQLClient gitHubGraphQLClient;

    @Mock
    private Mapper mapper;

    @Mock
    private LabelRepository labelRepository;

    @Mock
    private GitHubRepositoryStatisticsDTO statisticsDTO;

    private LabelInjectionService labelInjectionService;

    @BeforeEach
    public void setUp() {
        labelInjectionService =
                new LabelInjectionService(statisticsService, gitHubGraphQLClient, mapper, labelRepository);
    }

    @Test
    public void shouldSkipIfLabelCountsAreEqual() throws LabelInjectionException, GitHubGraphQLClientException {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
        when(statisticsDTO.getGitHubLabelCount()).thenReturn(5);
        when(labelRepository.count()).thenReturn(5L);

        labelInjectionService.injectLabels();

        verify(gitHubGraphQLClient, never()).getLabels();
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
        when(gitHubGraphQLClient.getLabels()).thenReturn(dtos);
        when(mapper.toEntity(dtos, Label.class)).thenReturn(entities);

        labelInjectionService.injectLabels();

        verify(labelRepository).saveAll(entities);
    }

    @Test
    public void shouldThrowLabelInjectionException_whenStatisticsAreUnavailable() {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(null);

        assertThrows(LabelInjectionException.class, () -> labelInjectionService.injectLabels());
    }

    @Test
    public void shouldThrowLabelInjectionException_whenClientFails() throws GitHubGraphQLClientException {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
        when(statisticsDTO.getGitHubLabelCount()).thenReturn(4);
        when(labelRepository.count()).thenReturn(1L);
        when(gitHubGraphQLClient.getLabels()).thenThrow(new GitHubGraphQLClientException("API fetch failed", null));

        assertThrows(LabelInjectionException.class, () -> labelInjectionService.injectLabels());
    }

    @Test
    public void shouldThrowLabelInjectionException_whenMappingFails() throws Exception {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
        when(statisticsDTO.getGitHubLabelCount()).thenReturn(10);
        when(labelRepository.count()).thenReturn(0L);
        when(gitHubGraphQLClient.getLabels()).thenReturn(Collections.emptySet());
        when(mapper.toEntity(anySet(), eq(Label.class))).thenThrow(new MappingException("Mapping failed", null));

        assertThrows(LabelInjectionException.class, () -> labelInjectionService.injectLabels());
    }

    @Test
    public void getAllLabelsMap_shouldReturnLabelsById() {
        Label bug = createLabel("l1", "bug", "A bug", "D73A4A");
        Label feature = createLabel("l2", "feature", "A feature", "007BFF");
        when(labelRepository.findAll()).thenReturn(List.of(bug, feature));

        Map<String, Label> result = labelInjectionService.getAllLabelsMap();

        assertEquals(2, result.size());
        assertEquals(bug, result.get("l1"));
        assertEquals(feature, result.get("l2"));
    }

    @Test
    public void getAllLabelsMap_shouldReturnEmptyMapWhenNoLabels() {
        when(labelRepository.findAll()).thenReturn(Collections.emptyList());

        assertTrue(labelInjectionService.getAllLabelsMap().isEmpty());
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
