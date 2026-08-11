package org.frankframework.insights.milestone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.frankframework.insights.common.enums.GitHubPropertyState;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.mapper.MappingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MilestoneQueryServiceTest {

    @Mock
    Mapper mapper;

    @Mock
    MilestoneRepository milestoneRepository;

    @InjectMocks
    MilestoneQueryService milestoneQueryService;

    private Milestone milestone1;

    @BeforeEach
    public void setUp() {
        milestone1 = new Milestone();
        milestone1.setId("m1");
        milestone1.setNumber(1);
        milestone1.setTitle("Milestone 1");
        milestone1.setState(GitHubPropertyState.OPEN);
    }

    @Test
    public void getAllMilestones_shouldReturnMappedSet() throws MappingException {
        Set<Milestone> milestones = Set.of(milestone1);
        Set<MilestoneResponse> responses = Set.of(
                new MilestoneResponse("m1", 1, "First", "https//example.com", GitHubPropertyState.OPEN, null, 0, 0));
        when(milestoneRepository.findAll()).thenReturn(milestones.stream().toList());
        when(mapper.toDTO(milestones, MilestoneResponse.class)).thenReturn(responses);

        Set<MilestoneResponse> result = milestoneQueryService.getAllMilestones();

        assertEquals(1, result.size());
        assertEquals("m1", result.iterator().next().id());
    }

    @Test
    public void getAllMilestones_shouldReturnEmptyIfNoneOpen() throws MappingException {
        when(milestoneRepository.findAll()).thenReturn(Collections.emptyList());
        when(mapper.toDTO(Collections.emptySet(), MilestoneResponse.class)).thenReturn(Collections.emptySet());

        Set<MilestoneResponse> result = milestoneQueryService.getAllMilestones();
        assertTrue(result.isEmpty());
    }

    @Test
    public void getAllMilestones_shouldThrowMappingException() throws MappingException {
        Set<Milestone> milestones = Set.of(milestone1);
        when(milestoneRepository.findAll()).thenReturn(milestones.stream().toList());
        when(mapper.toDTO(anySet(), eq(MilestoneResponse.class)))
                .thenThrow(new MappingException("failed mapping", null));

        assertThrows(MappingException.class, () -> milestoneQueryService.getAllMilestones());
    }

    @Test
    public void checkIfMilestoneExists_shouldReturnMilestone() throws MilestoneNotFoundException {
        when(milestoneRepository.findById("m1")).thenReturn(Optional.of(milestone1));
        Milestone found = milestoneQueryService.checkIfMilestoneExists("m1");
        assertEquals(milestone1, found);
    }

    @Test
    public void checkIfMilestoneExists_shouldThrowIfNotFound() {
        when(milestoneRepository.findById("notfound")).thenReturn(Optional.empty());
        assertThrows(MilestoneNotFoundException.class, () -> milestoneQueryService.checkIfMilestoneExists("notfound"));
    }
}
