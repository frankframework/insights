package org.frankframework.insights.milestone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.mapper.MappingException;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubClientException;
import org.frankframework.insights.github.GitHubPropertyState;
import org.frankframework.insights.github.GitHubRepositoryStatisticsDTO;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MilestoneServiceTest {

    @Mock
    GitHubRepositoryStatisticsService statisticsService;

    @Mock
    GitHubClient gitHubClient;

    @Mock
    Mapper mapper;

    @Mock
    MilestoneRepository milestoneRepository;

    @InjectMocks
    MilestoneService milestoneService;

    @Mock
    GitHubRepositoryStatisticsDTO statisticsDTO;

    private Milestone milestone1, milestone2;
    private MilestoneDTO milestoneDTO1, milestoneDTO2;

    @BeforeEach
    public void setUp() {
        milestone1 = new Milestone();
        milestone1.setId("m1");
        milestone1.setNumber(1);
        milestone1.setTitle("Milestone 1");
        milestone1.setState(GitHubPropertyState.OPEN);

        milestone2 = new Milestone();
        milestone2.setId("m2");
        milestone2.setNumber(2);
        milestone2.setTitle("Milestone 2");
        milestone2.setState(GitHubPropertyState.CLOSED);

        milestoneDTO1 = new MilestoneDTO("m1", 1, "First", "https//example.com", GitHubPropertyState.OPEN, null, 0, 0);
        milestoneDTO2 = new MilestoneDTO("m2", 2, "Second", "https//example.com", GitHubPropertyState.CLOSED, null, 0, 0);
    }

    @Test
    public void injectMilestones_shouldSkipIfCountsEqual() throws MilestoneInjectionException, GitHubClientException {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
        when(statisticsDTO.getGitHubMilestoneCount()).thenReturn(5);
        when(milestoneRepository.count()).thenReturn(5L);

        milestoneService.injectMilestones();

        verify(gitHubClient, never()).getMilestones();
        verify(milestoneRepository, never()).saveAll(anySet());
    }

    @Test
    public void injectMilestones_shouldSaveAllMilestones()
            throws MilestoneInjectionException, GitHubClientException, MappingException {
        Set<MilestoneDTO> DTOs = Set.of(milestoneDTO1, milestoneDTO2);
        Set<Milestone> entities = Set.of(milestone1, milestone2);
        List<Milestone> saved = List.of(milestone1, milestone2);

        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
        when(statisticsDTO.getGitHubMilestoneCount()).thenReturn(3);
        when(milestoneRepository.count()).thenReturn(2L);
        when(gitHubClient.getMilestones()).thenReturn(DTOs);
        when(mapper.toEntity(DTOs, Milestone.class)).thenReturn(entities);
        when(milestoneRepository.saveAll(entities)).thenReturn(saved);

        milestoneService.injectMilestones();

        verify(milestoneRepository).saveAll(entities);
    }

    @Test
    public void injectMilestones_shouldThrowOnException() throws GitHubClientException {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statisticsDTO);
        when(statisticsDTO.getGitHubMilestoneCount()).thenReturn(4);
        when(milestoneRepository.count()).thenReturn(1L);
        when(gitHubClient.getMilestones()).thenThrow(new RuntimeException("fail"));

        assertThrows(MilestoneInjectionException.class, () -> milestoneService.injectMilestones());
    }

    @Test
    public void getAllOpenMilestones_shouldReturnMappedSet() throws MappingException {
        Set<Milestone> openMilestones = Set.of(milestone1);
        Set<MilestoneResponse> responses = Set.of(new MilestoneResponse("m1", 1, "First", "https//example.com", GitHubPropertyState.OPEN, null, 0, 0));
        when(milestoneRepository.findAllByState(GitHubPropertyState.OPEN)).thenReturn(openMilestones);
        when(mapper.toDTO(openMilestones, MilestoneResponse.class)).thenReturn(responses);

        Set<MilestoneResponse> result = milestoneService.getAllOpenMilestones();

        assertEquals(1, result.size());
        assertEquals("m1", result.iterator().next().id());
    }

    @Test
    public void getAllOpenMilestones_shouldReturnEmptyIfNoneOpen() throws MappingException {
        when(milestoneRepository.findAllByState(GitHubPropertyState.OPEN)).thenReturn(Collections.emptySet());
        when(mapper.toDTO(Collections.emptySet(), MilestoneResponse.class)).thenReturn(Collections.emptySet());

        Set<MilestoneResponse> result = milestoneService.getAllOpenMilestones();
        assertTrue(result.isEmpty());
    }

    @Test
    public void getAllOpenMilestones_shouldThrowMappingException() throws MappingException {
        Set<Milestone> openMilestones = Set.of(milestone1);
        when(milestoneRepository.findAllByState(GitHubPropertyState.OPEN)).thenReturn(openMilestones);
        when(mapper.toDTO(anySet(), eq(MilestoneResponse.class)))
                .thenThrow(new MappingException("failed mapping", null));

        assertThrows(MappingException.class, () -> milestoneService.getAllOpenMilestones());
    }

    @Test
    public void getAllMilestonesMap_shouldReturnMap() {
        when(milestoneRepository.findAll()).thenReturn(List.of(milestone1, milestone2));
        Map<String, Milestone> result = milestoneService.getAllMilestonesMap();
        assertEquals(2, result.size());
        assertEquals(milestone1, result.get("m1"));
        assertEquals(milestone2, result.get("m2"));
    }

    @Test
    public void getAllMilestonesMap_shouldReturnEmptyMapIfNoMilestones() {
        when(milestoneRepository.findAll()).thenReturn(Collections.emptyList());
        Map<String, Milestone> result = milestoneService.getAllMilestonesMap();
        assertTrue(result.isEmpty());
    }

    @Test
    public void checkIfMilestoneExists_shouldReturnMilestone() throws MilestoneNotFoundException {
        when(milestoneRepository.findById("m1")).thenReturn(Optional.of(milestone1));
        Milestone found = milestoneService.checkIfMilestoneExists("m1");
        assertEquals(milestone1, found);
    }

    @Test
    public void checkIfMilestoneExists_shouldThrowIfNotFound() {
        when(milestoneRepository.findById("notfound")).thenReturn(Optional.empty());
        assertThrows(MilestoneNotFoundException.class, () -> milestoneService.checkIfMilestoneExists("notfound"));
    }
}
