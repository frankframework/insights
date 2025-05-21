package org.frankframework.insights.label;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabel;
import org.frankframework.insights.common.entityconnection.issuelabel.IssueLabelRepository;
import org.frankframework.insights.common.helper.ReleaseIssueHelperService;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.mapper.MappingException;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubClientException;
import org.frankframework.insights.github.GitHubRepositoryStatisticsDTO;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.frankframework.insights.issue.Issue;
import org.frankframework.insights.release.ReleaseNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LabelServiceTest {

	@Mock GitHubRepositoryStatisticsService statisticsService;
	@Mock GitHubClient gitHubClient;
	@Mock Mapper mapper;
	@Mock LabelRepository labelRepository;
	@Mock ReleaseIssueHelperService releaseIssueHelperService;
	@Mock IssueLabelRepository issueLabelRepository;

	@InjectMocks
	LabelService labelService;

	@Mock GitHubRepositoryStatisticsDTO statisticsDTO;

	private Label label1, label2;
	private LabelDTO labelDTO1, labelDTO2;
	private IssueLabel issueLabel1, issueLabel2;

	@BeforeEach
	void setUp() {
		label1 = new Label();
		label1.setId(UUID.randomUUID().toString());
		label1.setName("bug");
		label1.setDescription("Of type bug");
		label1.setColor("red");

		label2 = new Label();
		label2.setId(UUID.randomUUID().toString());
		label2.setName("feature");
		label2.setDescription("of type feature");
		label2.setColor("blue");

		labelDTO1 = new LabelDTO();
		labelDTO1.id = "l1";
		labelDTO1.name = "bug";
		labelDTO1.description = "desc";
		labelDTO1.color = "red";

		labelDTO2 = new LabelDTO();
		labelDTO2.id = "l2";
		labelDTO2.name = "feature";
		labelDTO2.description = "desc2";
		labelDTO2.color = "blue";

		issueLabel1 = new IssueLabel();
		issueLabel1.setLabel(label1);

		issueLabel2 = new IssueLabel();
		issueLabel2.setLabel(label2);
	}

	@Test
	public void injectLabels_shouldSkipIfCountsEqual() throws LabelInjectionException, GitHubClientException {
		when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
		when(statisticsDTO.getGitHubLabelCount()).thenReturn(5);
		when(labelRepository.count()).thenReturn(5L);

		labelService.injectLabels();

		verify(gitHubClient, never()).getLabels();
		verify(labelRepository, never()).saveAll(anySet());
	}

	@Test
	public void injectLabels_shouldSaveAllLabels() throws GitHubClientException, LabelInjectionException, MappingException {
		Set<LabelDTO> dtos = Set.of(labelDTO1, labelDTO2);
		Set<Label> entities = Set.of(label1, label2);
		List<Label> saved = List.of(label1, label2);

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
	public void getHighlightsByReleaseId_shouldReturnFilteredDTOs() throws Exception {
		Issue issueA = new Issue();
		issueA.setId("iA");
		Issue issueB = new Issue();
		issueB.setId("iB");

		Set<Issue> issues = Set.of(issueA, issueB);

		IssueLabel issueLabelA1 = mock(IssueLabel.class);
		when(issueLabelA1.getLabel()).thenReturn(label1);
		IssueLabel issueLabelA2 = mock(IssueLabel.class);
		when(issueLabelA2.getLabel()).thenReturn(label1);

		IssueLabel issueLabelB1 = mock(IssueLabel.class);
		when(issueLabelB1.getLabel()).thenReturn(label2);

		when(releaseIssueHelperService.getIssuesByReleaseId("r1")).thenReturn(issues);
		when(issueLabelRepository.findAllByIssue_Id("iA")).thenReturn(Set.of(issueLabelA1, issueLabelA2));
		when(issueLabelRepository.findAllByIssue_Id("iB")).thenReturn(Set.of(issueLabelB1));

		// Filtering: threshold = total * 0.05 = 3*0.05 = 0.15, so both labels are included
		Set<Label> filtered = Set.of(label1, label2);
		Set<LabelResponse> responses = Set.of(new LabelResponse("l1", "bug", "desc", "red"), new LabelResponse("l2", "feature", "desc2", "blue"));

		when(mapper.toDTO(filtered, LabelResponse.class)).thenReturn(responses);

		Set<LabelResponse> result = labelService.getHighlightsByReleaseId("r1");

		assertEquals(2, result.size());
		verify(mapper).toDTO(filtered, LabelResponse.class);
	}

	@Test
	public void getHighlightsByReleaseId_shouldReturnEmptyIfNoIssuesOrLabels() throws Exception {
		when(releaseIssueHelperService.getIssuesByReleaseId(anyString())).thenReturn(Collections.emptySet());
		when(mapper.toDTO(anySet(), eq(LabelResponse.class))).thenReturn(Collections.emptySet());

		Set<LabelResponse> result = labelService.getHighlightsByReleaseId("relX");
		assertTrue(result.isEmpty());
	}

	@Test
	public void getHighlightsByReleaseId_shouldThrowIfReleaseNotFound() throws Exception {
		when(releaseIssueHelperService.getIssuesByReleaseId("notfound"))
				.thenThrow(new ReleaseNotFoundException("Release not found", null));
		assertThrows(ReleaseNotFoundException.class, () -> labelService.getHighlightsByReleaseId("notfound"));
	}

	@Test
	public void getHighlightsByReleaseId_shouldThrowIfMappingFails() throws Exception {
		Issue issueA = new Issue();
		issueA.setId("iA");
		Set<Issue> issues = Set.of(issueA);

		IssueLabel issueLabelA1 = mock(IssueLabel.class);
		when(issueLabelA1.getLabel()).thenReturn(label1);

		when(releaseIssueHelperService.getIssuesByReleaseId("failMap")).thenReturn(issues);
		when(issueLabelRepository.findAllByIssue_Id("iA")).thenReturn(Set.of(issueLabelA1));
		when(mapper.toDTO(anySet(), eq(LabelResponse.class))).thenThrow(new MappingException("Mapping failed", null));

		assertThrows(MappingException.class, () -> labelService.getHighlightsByReleaseId("failMap"));
	}

	@Test
	public void getLabelsByIssueId_shouldReturnLabels() {
		when(issueLabelRepository.findAllByIssue_Id("i1")).thenReturn(Set.of(issueLabel1, issueLabel2));
		Set<Label> result = labelService.getLabelsByIssueId("i1");
		assertEquals(2, result.size());
		assertTrue(result.contains(label1));
		assertTrue(result.contains(label2));
	}

	@Test
	public void getLabelsByIssueId_shouldReturnEmptyIfNoLabels() {
		when(issueLabelRepository.findAllByIssue_Id("i99")).thenReturn(Set.of());
		Set<Label> result = labelService.getLabelsByIssueId("i99");
		assertTrue(result.isEmpty());
	}

	@Test
	public void saveLabels_andShouldLogInfo() throws GitHubClientException, MappingException, LabelInjectionException {
		Set<Label> labels = Set.of(label1, label2);
		when(labelRepository.saveAll(labels)).thenReturn(List.of(label1, label2));

		when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
		when(statisticsDTO.getGitHubLabelCount()).thenReturn(1);
		when(labelRepository.count()).thenReturn(0L);
		when(gitHubClient.getLabels()).thenReturn(Set.of(labelDTO1, labelDTO2));
		when(mapper.toEntity(anySet(), eq(Label.class))).thenReturn(labels);
		labelService.injectLabels();
		verify(labelRepository).saveAll(labels);
	}
}
