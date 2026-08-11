package org.frankframework.insights.milestone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.frankframework.insights.common.enums.GitHubPropertyState;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.mapper.MappingException;
import org.frankframework.insights.github.graphql.GitHubGraphQLClient;
import org.frankframework.insights.github.graphql.GitHubGraphQLClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MilestoneInjectionServiceTest {

    @Mock
    GitHubGraphQLClient gitHubGraphQLClient;

    @Mock
    Mapper mapper;

    @Mock
    MilestoneRepository milestoneRepository;

    @InjectMocks
    MilestoneInjectionService milestoneInjectionService;

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
        milestoneDTO2 =
                new MilestoneDTO("m2", 2, "Second", "https//example.com", GitHubPropertyState.CLOSED, null, 0, 0);
    }

    @Test
    public void injectMilestones_shouldSaveAllMilestones()
            throws MilestoneInjectionException, GitHubGraphQLClientException, MappingException {
        Set<MilestoneDTO> DTOs = Set.of(milestoneDTO1, milestoneDTO2);
        Set<Milestone> entities = Set.of(milestone1, milestone2);
        List<Milestone> saved = List.of(milestone1, milestone2);

        when(gitHubGraphQLClient.getMilestones()).thenReturn(DTOs);
        when(mapper.toEntity(DTOs, Milestone.class)).thenReturn(entities);
        when(milestoneRepository.saveAll(entities)).thenReturn(saved);

        milestoneInjectionService.injectMilestones();

        verify(milestoneRepository).saveAll(entities);
    }

    @Test
    public void injectMilestones_shouldThrowOnException() throws GitHubGraphQLClientException {
        when(gitHubGraphQLClient.getMilestones()).thenThrow(new RuntimeException("fail"));

        assertThrows(MilestoneInjectionException.class, () -> milestoneInjectionService.injectMilestones());
    }

    @Test
    public void getAllMilestonesMap_shouldReturnMap() {
        when(milestoneRepository.findAll()).thenReturn(List.of(milestone1, milestone2));
        Map<String, Milestone> result = milestoneInjectionService.getAllMilestonesMap();
        assertEquals(2, result.size());
        assertEquals(milestone1, result.get("m1"));
        assertEquals(milestone2, result.get("m2"));
    }

    @Test
    public void getAllMilestonesMap_shouldReturnEmptyMapIfNoMilestones() {
        when(milestoneRepository.findAll()).thenReturn(Collections.emptyList());
        Map<String, Milestone> result = milestoneInjectionService.getAllMilestonesMap();
        assertTrue(result.isEmpty());
    }
}
