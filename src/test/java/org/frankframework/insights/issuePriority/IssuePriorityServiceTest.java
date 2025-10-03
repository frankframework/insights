package org.frankframework.insights.issuePriority;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.properties.GitHubProperties;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubClientException;
import org.frankframework.insights.github.GitHubPrioritySingleSelectDTO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IssuePriorityServiceTest {

    @Mock
    IssuePriorityRepository issuePriorityRepository;

    @Mock
    GitHubClient gitHubClient;

    @Mock
    Mapper mapper;

    @Mock
    GitHubProperties gitHubProperties;

    @InjectMocks
    IssuePriorityService issuePriorityService;

    private IssuePriorityDTO dto1, dto2;
    private IssuePriority entity1, entity2;

    @BeforeEach
    public void setup() {
        when(gitHubProperties.getProjectId()).thenReturn("project123");
        issuePriorityService =
                new IssuePriorityService(issuePriorityRepository, gitHubClient, mapper, gitHubProperties);

        dto1 = new IssuePriorityDTO("p1", "High", "High Priority", "red");

        dto2 = new IssuePriorityDTO("p2", "Low", "Low Priority", "green");

        entity1 = new IssuePriority();
        entity1.setId("p1");
        entity1.setName("High");
        entity1.setDescription("High Priority");
        entity1.setColor("red");

        entity2 = new IssuePriority();
        entity2.setId("p2");
        entity2.setName("Low");
        entity2.setDescription("Low Priority");
        entity2.setColor("green");
    }

    @Test
    public void injectIssuePriorities_shouldSkipIfRepoNotEmpty()
            throws GitHubClientException, IssuePriorityInjectionException {
        when(issuePriorityRepository.count()).thenReturn(1L);
        issuePriorityService.injectIssuePriorities();
        verify(gitHubClient, never()).getIssuePriorities(any());
        verify(issuePriorityRepository, never()).saveAll(anySet());
    }

    @Test
    public void injectIssuePriorities_shouldSaveAllPrioritiesFromCorrectField()
            throws GitHubClientException, IssuePriorityInjectionException {
        when(issuePriorityRepository.count()).thenReturn(0L);

        GitHubPrioritySingleSelectDTO.SingleSelectObject priorityField =
                new GitHubPrioritySingleSelectDTO.SingleSelectObject("priority", List.of(dto1, dto2));

        Set<GitHubPrioritySingleSelectDTO.SingleSelectObject> githubDtos = Set.of(priorityField);

        when(gitHubClient.getIssuePriorities("project123")).thenReturn(githubDtos);
        when(mapper.toEntity(dto1, IssuePriority.class)).thenReturn(entity1);
        when(mapper.toEntity(dto2, IssuePriority.class)).thenReturn(entity2);
        when(issuePriorityRepository.saveAll(anySet())).thenReturn(List.of(entity1, entity2));

        issuePriorityService.injectIssuePriorities();

        verify(issuePriorityRepository)
                .saveAll(argThat(
                        (Set<IssuePriority> set) -> set.size() == 2 && set.contains(entity1) && set.contains(entity2)));
    }

    @Test
    public void injectIssuePriorities_shouldHandleWrongFieldName()
            throws GitHubClientException, IssuePriorityInjectionException {
        when(issuePriorityRepository.count()).thenReturn(0L);

        GitHubPrioritySingleSelectDTO.SingleSelectObject wrongField =
                new GitHubPrioritySingleSelectDTO.SingleSelectObject("urgency", List.of(dto1, dto2));

        Set<GitHubPrioritySingleSelectDTO.SingleSelectObject> githubDtos = Set.of(wrongField);

        when(gitHubClient.getIssuePriorities("project123")).thenReturn(githubDtos);
        when(issuePriorityRepository.saveAll(anySet())).thenReturn(List.of());

        issuePriorityService.injectIssuePriorities();
        verify(issuePriorityRepository).saveAll(argThat((Set<IssuePriority> set) -> set.isEmpty()));
    }

    @Test
    public void injectIssuePriorities_shouldHandleNullName()
            throws GitHubClientException, IssuePriorityInjectionException {
        when(issuePriorityRepository.count()).thenReturn(0L);

        GitHubPrioritySingleSelectDTO.SingleSelectObject nullName =
                new GitHubPrioritySingleSelectDTO.SingleSelectObject(null, List.of(dto1, dto2));

        Set<GitHubPrioritySingleSelectDTO.SingleSelectObject> githubDtos = Set.of(nullName);

        when(gitHubClient.getIssuePriorities("project123")).thenReturn(githubDtos);
        when(issuePriorityRepository.saveAll(anySet())).thenReturn(List.of());

        issuePriorityService.injectIssuePriorities();
        verify(issuePriorityRepository).saveAll(argThat((Set<IssuePriority> set) -> set.isEmpty()));
    }

    @Test
    public void injectIssuePriorities_shouldHandleNullOptions()
            throws GitHubClientException, IssuePriorityInjectionException {
        when(issuePriorityRepository.count()).thenReturn(0L);

        GitHubPrioritySingleSelectDTO.SingleSelectObject nullOptions =
                new GitHubPrioritySingleSelectDTO.SingleSelectObject("priority", null);

        Set<GitHubPrioritySingleSelectDTO.SingleSelectObject> githubDtos = Set.of(nullOptions);

        when(gitHubClient.getIssuePriorities("project123")).thenReturn(githubDtos);
        when(issuePriorityRepository.saveAll(anySet())).thenReturn(List.of());

        issuePriorityService.injectIssuePriorities();
        verify(issuePriorityRepository).saveAll(argThat((Set<IssuePriority> set) -> set.isEmpty()));
    }

    @Test
    public void injectIssuePriorities_shouldHandleEmptyOptions()
            throws GitHubClientException, IssuePriorityInjectionException {
        when(issuePriorityRepository.count()).thenReturn(0L);

        GitHubPrioritySingleSelectDTO.SingleSelectObject emptyOptions =
                new GitHubPrioritySingleSelectDTO.SingleSelectObject("priority", List.of());

        Set<GitHubPrioritySingleSelectDTO.SingleSelectObject> githubDtos = Set.of(emptyOptions);

        when(gitHubClient.getIssuePriorities("project123")).thenReturn(githubDtos);
        when(issuePriorityRepository.saveAll(anySet())).thenReturn(List.of());

        issuePriorityService.injectIssuePriorities();
        verify(issuePriorityRepository).saveAll(argThat((Set<IssuePriority> set) -> set.isEmpty()));
    }

    @Test
    public void injectIssuePriorities_shouldHandleNoPriorityFieldAtAll()
            throws GitHubClientException, IssuePriorityInjectionException {
        when(issuePriorityRepository.count()).thenReturn(0L);

        Set<GitHubPrioritySingleSelectDTO.SingleSelectObject> githubDtos = Set.of();

        when(gitHubClient.getIssuePriorities("project123")).thenReturn(githubDtos);
        when(issuePriorityRepository.saveAll(anySet())).thenReturn(List.of());

        issuePriorityService.injectIssuePriorities();
        verify(issuePriorityRepository).saveAll(argThat((Set<IssuePriority> set) -> set.isEmpty()));
    }

    @Test
    public void injectIssuePriorities_shouldWrapGitHubClientException() throws GitHubClientException {
        when(issuePriorityRepository.count()).thenReturn(0L);
        when(gitHubClient.getIssuePriorities(anyString())).thenThrow(new RuntimeException("GitHub fail"));

        IssuePriorityInjectionException ex =
                assertThrows(IssuePriorityInjectionException.class, () -> issuePriorityService.injectIssuePriorities());
        assertTrue(ex.getMessage().contains("Error while injecting GitHub issue priorities"));
        assertNotNull(ex.getCause());
    }

    @Test
    public void injectIssuePriorities_shouldWrapMapperException() throws GitHubClientException {
        when(issuePriorityRepository.count()).thenReturn(0L);

        GitHubPrioritySingleSelectDTO.SingleSelectObject priorityField =
                new GitHubPrioritySingleSelectDTO.SingleSelectObject("priority", List.of(dto1));

        Set<GitHubPrioritySingleSelectDTO.SingleSelectObject> githubDtos = Set.of(priorityField);

        when(gitHubClient.getIssuePriorities("project123")).thenReturn(githubDtos);
        when(mapper.toEntity(dto1, IssuePriority.class)).thenThrow(new RuntimeException("Mapping failed"));

        IssuePriorityInjectionException ex =
                assertThrows(IssuePriorityInjectionException.class, () -> issuePriorityService.injectIssuePriorities());
        assertTrue(ex.getMessage().contains("Error while injecting GitHub issue priorities"));
    }

    @Test
    public void getAllIssuePrioritiesMap_shouldReturnCorrectMap() {
        when(issuePriorityRepository.findAll()).thenReturn(List.of(entity1, entity2));
        Map<String, IssuePriority> result = issuePriorityService.getAllIssuePrioritiesMap();
        assertEquals(2, result.size());
        assertEquals(entity1, result.get("p1"));
        assertEquals(entity2, result.get("p2"));
    }

    @Test
    public void injectIssuePriorities_shouldSaveEmptyIfNoDTOSFromGitHub()
            throws GitHubClientException, IssuePriorityInjectionException {
        when(issuePriorityRepository.count()).thenReturn(0L);
        when(gitHubClient.getIssuePriorities(anyString())).thenReturn(Collections.emptySet());
        when(issuePriorityRepository.saveAll(Collections.emptySet())).thenReturn(Collections.emptyList());

        issuePriorityService.injectIssuePriorities();
        verify(issuePriorityRepository).saveAll(argThat((Set<IssuePriority> set) -> set.isEmpty()));
    }
}
