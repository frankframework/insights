package org.frankframework.insights.issue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabel;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabelRepository;
import org.frankframework.insights.common.helper.IssueLabelHelperService;
import org.frankframework.insights.github.GitHubEdgesDTO;
import org.frankframework.insights.github.GitHubNodeDTO;
import org.frankframework.insights.label.Label;
import org.frankframework.insights.label.LabelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

public class IssueLabelHelperServiceTest {

    @Mock
    private IssueLabelRepository issueLabelRepository;

    @Mock
    private LabelRepository labelRepository;

    @InjectMocks
    private IssueLabelHelperService issueLabelHelperService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        issueLabelHelperService = new IssueLabelHelperService(issueLabelRepository, labelRepository);
    }

    @Test
    public void saveIssueLabels_shouldSaveAllLabelsForIssues() {
        Issue issue = new Issue();
        issue.setId("i1");

        Label label = new Label();
        label.setId("l1");
        label.setName("bug");

        var labelDTO = new org.frankframework.insights.label.LabelDTO();
        labelDTO.id = "l1";

        var nodeDTO = new GitHubNodeDTO<org.frankframework.insights.label.LabelDTO>();
        nodeDTO.setNode(labelDTO);

        var edges = new GitHubEdgesDTO<org.frankframework.insights.label.LabelDTO>();
        edges.setEdges(List.of(nodeDTO));

        IssueDTO dto = new IssueDTO("i1", 101, "Issue", null, null, null, edges, null, null);

        Map<String, IssueDTO> issueDtoMap = Map.of("i1", dto);

        when(labelRepository.findAll()).thenReturn(List.of(label));

        issueLabelHelperService.saveIssueLabels(Set.of(issue), issueDtoMap);

        ArgumentCaptor<Collection<IssueLabel>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(issueLabelRepository).saveAll(captor.capture());
        Collection<IssueLabel> savedLabels = captor.getValue();
        assertEquals(1, savedLabels.size());
        IssueLabel issueLabel = savedLabels.iterator().next();
        assertEquals(issue, issueLabel.getIssue());
        assertEquals(label, issueLabel.getLabel());
    }

    @Test
    public void saveIssueLabels_noLabelsInDTO_shouldNotSaveAnything() {
        Issue issue = new Issue();
        issue.setId("i1");
        IssueDTO dto = mock(IssueDTO.class);
        when(dto.hasLabels()).thenReturn(false);

        Map<String, IssueDTO> issueDtoMap = Map.of("i1", dto);

        when(labelRepository.findAll()).thenReturn(Collections.emptyList());

        issueLabelHelperService.saveIssueLabels(Set.of(issue), issueDtoMap);

        verify(issueLabelRepository, never()).saveAll(anyCollection());
    }

    @Test
    public void saveIssueLabels_nullDTO_shouldNotSaveAnything() {
        Issue issue = new Issue();
        issue.setId("i1");

        IssueDTO dto = new IssueDTO("i1", 101, "Issue", null, null, null, null, null, null);

        Map<String, IssueDTO> issueDtoMap = Map.of("i1", dto);

        when(labelRepository.findAll()).thenReturn(Collections.emptyList());

        issueLabelHelperService.saveIssueLabels(Set.of(issue), issueDtoMap);

        verify(issueLabelRepository, never()).saveAll(anyCollection());
    }

    @Test
    public void saveIssueLabels_labelNotFoundInMap_shouldSkipThatLabel() {
        Issue issue = new Issue();
        issue.setId("i1");

        var labelDTO = new org.frankframework.insights.label.LabelDTO();
        labelDTO.id = "l1";

        var nodeDTO = new GitHubNodeDTO<org.frankframework.insights.label.LabelDTO>();
        nodeDTO.setNode(labelDTO);

        var edges = new GitHubEdgesDTO<org.frankframework.insights.label.LabelDTO>();
        edges.setEdges(List.of(nodeDTO));

        IssueDTO dto = new IssueDTO("i1", 101, "Issue", null, null, null, edges, null, null);

        Map<String, IssueDTO> issueDtoMap = Map.of("i1", dto);

        when(labelRepository.findAll()).thenReturn(Collections.emptyList());

        issueLabelHelperService.saveIssueLabels(Set.of(issue), issueDtoMap);

        verify(issueLabelRepository).saveAll(argThat(iter -> !iter.iterator().hasNext()));
    }

    @Test
    public void getAllLabelsMap_returnsMapOfLabels() {
        Label label1 = new Label();
        label1.setId("a");
        Label label2 = new Label();
        label2.setId("b");
        when(labelRepository.findAll()).thenReturn(List.of(label1, label2));

        Map<String, Label> labelMap = issueLabelHelperService.getAllLabelsMap();
        assertEquals(2, labelMap.size());
        assertEquals(label1, labelMap.get("a"));
        assertEquals(label2, labelMap.get("b"));
    }

    @Test
    public void getAllLabelsMap_emptyList_returnsEmptyMap() {
        when(labelRepository.findAll()).thenReturn(Collections.emptyList());
        Map<String, Label> labelMap = issueLabelHelperService.getAllLabelsMap();
        assertTrue(labelMap.isEmpty());
    }
}
