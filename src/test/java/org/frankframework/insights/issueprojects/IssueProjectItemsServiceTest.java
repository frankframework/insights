package org.frankframework.insights.issueprojects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.properties.GitHubProperties;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubClientException;
import org.frankframework.insights.github.GitHubSingleSelectDTO;
import org.frankframework.insights.github.GitHubSingleSelectProjectItemDTO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IssueProjectItemsServiceTest {

    @Mock
    IssuePriorityRepository issuePriorityRepository;

    @Mock
    IssueStateRepository issueStateRepository;

    @Mock
    GitHubClient gitHubClient;

    @Mock
    Mapper mapper;

    @Mock
    GitHubProperties gitHubProperties;

    @InjectMocks
    IssueProjectItemsService issueProjectItemsService;

    private GitHubSingleSelectProjectItemDTO priorityDto1, priorityDto2;
    private IssuePriority priorityEntity1, priorityEntity2;
    private GitHubSingleSelectProjectItemDTO stateDto1, stateDto2;
    private IssueState stateEntity1, stateEntity2;

    @BeforeEach
    public void setup() {
        when(gitHubProperties.getProjectId()).thenReturn("project123");
        issueProjectItemsService = new IssueProjectItemsService(
                issuePriorityRepository, issueStateRepository, gitHubClient, mapper, gitHubProperties);

        priorityDto1 = new GitHubSingleSelectProjectItemDTO("p1", "High", "red", "High Priority");
        priorityDto2 = new GitHubSingleSelectProjectItemDTO("p2", "Low", "green", "Low Priority");

        priorityEntity1 = new IssuePriority();
        priorityEntity1.setId("p1");
        priorityEntity1.setName("High");
        priorityEntity1.setColor("red");
        priorityEntity1.setDescription("High Priority");

        priorityEntity2 = new IssuePriority();
        priorityEntity2.setId("p2");
        priorityEntity2.setName("Low");
        priorityEntity2.setColor("green");
        priorityEntity2.setDescription("Low Priority");

        stateDto1 = new GitHubSingleSelectProjectItemDTO("s1", "In Progress", "blue", "Work in progress");
        stateDto2 = new GitHubSingleSelectProjectItemDTO("s2", "Done", "green", "Completed work");

        stateEntity1 = new IssueState();
        stateEntity1.setId("s1");
        stateEntity1.setName("In Progress");
        stateEntity1.setColor("blue");
        stateEntity1.setDescription("Work in progress");

        stateEntity2 = new IssueState();
        stateEntity2.setId("s2");
        stateEntity2.setName("Done");
        stateEntity2.setColor("green");
        stateEntity2.setDescription("Completed work");
    }

    @Test
    public void injectIssueProjectItems_shouldSkipIfRepoNotEmpty()
            throws GitHubClientException, IssueProjectItemInjectionException {
        when(issuePriorityRepository.count()).thenReturn(1L);
        when(issueStateRepository.count()).thenReturn(1L);
        issueProjectItemsService.injectIssueProjectItems();
        verify(gitHubClient, never()).getIssueProjectItems(any());
        verify(issuePriorityRepository, never()).saveAll(anySet());
        verify(issueStateRepository, never()).saveAll(anySet());
    }

    @Test
    public void injectIssueProjectItems_shouldSaveAllPrioritiesAndStatesFromCorrectFields()
            throws GitHubClientException, IssueProjectItemInjectionException {
        when(issuePriorityRepository.count()).thenReturn(0L);
        when(issueStateRepository.count()).thenReturn(0L);

        GitHubSingleSelectDTO.SingleSelectObject priorityField =
                new GitHubSingleSelectDTO.SingleSelectObject("priority", List.of(priorityDto1, priorityDto2));
        GitHubSingleSelectDTO.SingleSelectObject stateField =
                new GitHubSingleSelectDTO.SingleSelectObject("Status", List.of(stateDto1, stateDto2));

        Set<GitHubSingleSelectDTO.SingleSelectObject> githubDtos = Set.of(priorityField, stateField);

        when(gitHubClient.getIssueProjectItems("project123")).thenReturn(githubDtos);
        when(mapper.toEntity(priorityDto1, IssuePriority.class)).thenReturn(priorityEntity1);
        when(mapper.toEntity(priorityDto2, IssuePriority.class)).thenReturn(priorityEntity2);
        when(mapper.toEntity(any(GitHubSingleSelectProjectItemDTO.class), eq(IssueState.class)))
                .thenAnswer(invocation -> {
                    GitHubSingleSelectProjectItemDTO dto = invocation.getArgument(0);
                    if (dto.id().equals("s1")) return stateEntity1;
                    if (dto.id().equals("s2")) return stateEntity2;
                    return null;
                });
        when(issuePriorityRepository.saveAll(anySet())).thenReturn(List.of(priorityEntity1, priorityEntity2));
        when(issueStateRepository.saveAll(anySet())).thenReturn(List.of(stateEntity1, stateEntity2));

        issueProjectItemsService.injectIssueProjectItems();

        verify(issuePriorityRepository)
                .saveAll(argThat((Set<IssuePriority> set) ->
                        set.size() == 2 && set.contains(priorityEntity1) && set.contains(priorityEntity2)));
        verify(issueStateRepository)
                .saveAll(argThat((Set<IssueState> set) ->
                        set.size() == 2 && set.contains(stateEntity1) && set.contains(stateEntity2)));
    }

    @Test
    public void injectIssueProjectItems_shouldHandleWrongFieldName()
            throws GitHubClientException, IssueProjectItemInjectionException {
        when(issuePriorityRepository.count()).thenReturn(0L);
        when(issueStateRepository.count()).thenReturn(0L);

        GitHubSingleSelectDTO.SingleSelectObject wrongField =
                new GitHubSingleSelectDTO.SingleSelectObject("urgency", List.of(priorityDto1, priorityDto2));

        Set<GitHubSingleSelectDTO.SingleSelectObject> githubDtos = Set.of(wrongField);

        when(gitHubClient.getIssueProjectItems("project123")).thenReturn(githubDtos);
        when(issuePriorityRepository.saveAll(anySet())).thenReturn(List.of());
        when(issueStateRepository.saveAll(anySet())).thenReturn(List.of());

        issueProjectItemsService.injectIssueProjectItems();
        verify(issuePriorityRepository).saveAll(argThat((Set<IssuePriority> set) -> set.isEmpty()));
        verify(issueStateRepository).saveAll(argThat((Set<IssueState> set) -> set.isEmpty()));
    }

    @Test
    public void injectIssueProjectItems_shouldHandleNullName()
            throws GitHubClientException, IssueProjectItemInjectionException {
        when(issuePriorityRepository.count()).thenReturn(0L);
        when(issueStateRepository.count()).thenReturn(0L);

        GitHubSingleSelectDTO.SingleSelectObject nullName =
                new GitHubSingleSelectDTO.SingleSelectObject(null, List.of(priorityDto1, priorityDto2));

        Set<GitHubSingleSelectDTO.SingleSelectObject> githubDtos = Set.of(nullName);

        when(gitHubClient.getIssueProjectItems("project123")).thenReturn(githubDtos);
        when(issuePriorityRepository.saveAll(anySet())).thenReturn(List.of());
        when(issueStateRepository.saveAll(anySet())).thenReturn(List.of());

        issueProjectItemsService.injectIssueProjectItems();
        verify(issuePriorityRepository).saveAll(argThat((Set<IssuePriority> set) -> set.isEmpty()));
        verify(issueStateRepository).saveAll(argThat((Set<IssueState> set) -> set.isEmpty()));
    }

    @Test
    public void injectIssueProjectItems_shouldHandleNullOptions()
            throws GitHubClientException, IssueProjectItemInjectionException {
        when(issuePriorityRepository.count()).thenReturn(0L);
        when(issueStateRepository.count()).thenReturn(0L);

        GitHubSingleSelectDTO.SingleSelectObject nullOptions =
                new GitHubSingleSelectDTO.SingleSelectObject("priority", null);

        Set<GitHubSingleSelectDTO.SingleSelectObject> githubDtos = Set.of(nullOptions);

        when(gitHubClient.getIssueProjectItems("project123")).thenReturn(githubDtos);
        when(issuePriorityRepository.saveAll(anySet())).thenReturn(List.of());
        when(issueStateRepository.saveAll(anySet())).thenReturn(List.of());

        issueProjectItemsService.injectIssueProjectItems();
        verify(issuePriorityRepository).saveAll(argThat((Set<IssuePriority> set) -> set.isEmpty()));
        verify(issueStateRepository).saveAll(argThat((Set<IssueState> set) -> set.isEmpty()));
    }

    @Test
    public void injectIssueProjectItems_shouldHandleEmptyOptions()
            throws GitHubClientException, IssueProjectItemInjectionException {
        when(issuePriorityRepository.count()).thenReturn(0L);
        when(issueStateRepository.count()).thenReturn(0L);

        GitHubSingleSelectDTO.SingleSelectObject emptyOptions =
                new GitHubSingleSelectDTO.SingleSelectObject("priority", List.of());

        Set<GitHubSingleSelectDTO.SingleSelectObject> githubDtos = Set.of(emptyOptions);

        when(gitHubClient.getIssueProjectItems("project123")).thenReturn(githubDtos);
        when(issuePriorityRepository.saveAll(anySet())).thenReturn(List.of());
        when(issueStateRepository.saveAll(anySet())).thenReturn(List.of());

        issueProjectItemsService.injectIssueProjectItems();
        verify(issuePriorityRepository).saveAll(argThat((Set<IssuePriority> set) -> set.isEmpty()));
        verify(issueStateRepository).saveAll(argThat((Set<IssueState> set) -> set.isEmpty()));
    }

    @Test
    public void injectIssueProjectItems_shouldHandleNoPriorityFieldAtAll()
            throws GitHubClientException, IssueProjectItemInjectionException {
        when(issuePriorityRepository.count()).thenReturn(0L);
        when(issueStateRepository.count()).thenReturn(0L);

        Set<GitHubSingleSelectDTO.SingleSelectObject> githubDtos = Set.of();

        when(gitHubClient.getIssueProjectItems("project123")).thenReturn(githubDtos);
        when(issuePriorityRepository.saveAll(anySet())).thenReturn(List.of());
        when(issueStateRepository.saveAll(anySet())).thenReturn(List.of());

        issueProjectItemsService.injectIssueProjectItems();
        verify(issuePriorityRepository).saveAll(argThat((Set<IssuePriority> set) -> set.isEmpty()));
        verify(issueStateRepository).saveAll(argThat((Set<IssueState> set) -> set.isEmpty()));
    }

    @Test
    public void injectIssueProjectItems_shouldWrapGitHubClientException() throws GitHubClientException {
        when(issuePriorityRepository.count()).thenReturn(0L);
        when(issueStateRepository.count()).thenReturn(0L);
        when(gitHubClient.getIssueProjectItems(anyString())).thenThrow(new RuntimeException("GitHub fail"));

        IssueProjectItemInjectionException ex = assertThrows(
                IssueProjectItemInjectionException.class, () -> issueProjectItemsService.injectIssueProjectItems());
        assertTrue(ex.getMessage().contains("Error while injecting GitHub issue project items"));
        assertNotNull(ex.getCause());
    }

    @Test
    public void injectIssueProjectItems_shouldWrapPriorityMapperException() throws GitHubClientException {
        when(issuePriorityRepository.count()).thenReturn(0L);
        when(issueStateRepository.count()).thenReturn(0L);

        GitHubSingleSelectDTO.SingleSelectObject priorityField =
                new GitHubSingleSelectDTO.SingleSelectObject("priority", List.of(priorityDto1));

        Set<GitHubSingleSelectDTO.SingleSelectObject> githubDtos = Set.of(priorityField);

        when(gitHubClient.getIssueProjectItems("project123")).thenReturn(githubDtos);
        when(mapper.toEntity(priorityDto1, IssuePriority.class)).thenThrow(new RuntimeException("Mapping failed"));

        IssueProjectItemInjectionException ex = assertThrows(
                IssueProjectItemInjectionException.class, () -> issueProjectItemsService.injectIssueProjectItems());
        assertTrue(ex.getMessage().contains("Error while injecting GitHub issue project items"));
    }

    @Test
    public void injectIssueProjectItems_shouldWrapStateMapperException() throws GitHubClientException {
        when(issuePriorityRepository.count()).thenReturn(0L);
        when(issueStateRepository.count()).thenReturn(0L);

        GitHubSingleSelectDTO.SingleSelectObject priorityField =
                new GitHubSingleSelectDTO.SingleSelectObject("priority", List.of(priorityDto1));
        GitHubSingleSelectDTO.SingleSelectObject stateField =
                new GitHubSingleSelectDTO.SingleSelectObject("Status", List.of(stateDto1));

        Set<GitHubSingleSelectDTO.SingleSelectObject> githubDtos = Set.of(priorityField, stateField);

        when(gitHubClient.getIssueProjectItems("project123")).thenReturn(githubDtos);
        when(mapper.toEntity(priorityDto1, IssuePriority.class)).thenReturn(priorityEntity1);
        when(issuePriorityRepository.saveAll(anySet())).thenReturn(List.of(priorityEntity1));
        when(mapper.toEntity(any(GitHubSingleSelectProjectItemDTO.class), eq(IssueState.class)))
                .thenThrow(new RuntimeException("Mapping failed"));

        IssueProjectItemInjectionException ex = assertThrows(
                IssueProjectItemInjectionException.class, () -> issueProjectItemsService.injectIssueProjectItems());
        assertTrue(ex.getMessage().contains("Error while injecting GitHub issue project items"));
    }

    @Test
    public void getAllIssuePrioritiesMap_shouldReturnCorrectMap() {
        when(issuePriorityRepository.findAll()).thenReturn(List.of(priorityEntity1, priorityEntity2));
        Map<String, IssuePriority> result = issueProjectItemsService.getAllIssuePrioritiesMap();
        assertEquals(2, result.size());
        assertEquals(priorityEntity1, result.get("p1"));
        assertEquals(priorityEntity2, result.get("p2"));
    }

    @Test
    public void getAllIssueStatesMap_shouldReturnCorrectMap() {
        when(issueStateRepository.findAll()).thenReturn(List.of(stateEntity1, stateEntity2));
        Map<String, IssueState> result = issueProjectItemsService.getAllIssueStatesMap();
        assertEquals(2, result.size());
        assertEquals(stateEntity1, result.get("s1"));
        assertEquals(stateEntity2, result.get("s2"));
    }

    @Test
    public void injectIssueProjectItems_shouldSaveEmptyIfNoDTOSFromGitHub()
            throws GitHubClientException, IssueProjectItemInjectionException {
        when(issuePriorityRepository.count()).thenReturn(0L);
        when(issueStateRepository.count()).thenReturn(0L);
        when(gitHubClient.getIssueProjectItems(anyString())).thenReturn(Collections.emptySet());
        when(issuePriorityRepository.saveAll(Collections.emptySet())).thenReturn(Collections.emptyList());
        when(issueStateRepository.saveAll(Collections.emptySet())).thenReturn(Collections.emptyList());

        issueProjectItemsService.injectIssueProjectItems();
        verify(issuePriorityRepository).saveAll(argThat((Set<IssuePriority> set) -> set.isEmpty()));
        verify(issueStateRepository).saveAll(argThat((Set<IssueState> set) -> set.isEmpty()));
    }

    @Test
    public void injectIssueProjectItems_shouldOnlySavePrioritiesWhenStatesAlreadyExist()
            throws GitHubClientException, IssueProjectItemInjectionException {
        when(issuePriorityRepository.count()).thenReturn(0L).thenReturn(0L);
        when(issueStateRepository.count()).thenReturn(1L);

        GitHubSingleSelectDTO.SingleSelectObject priorityField =
                new GitHubSingleSelectDTO.SingleSelectObject("Priority", List.of(priorityDto1, priorityDto2));
        GitHubSingleSelectDTO.SingleSelectObject stateField =
                new GitHubSingleSelectDTO.SingleSelectObject("Status", List.of(stateDto1, stateDto2));

        Set<GitHubSingleSelectDTO.SingleSelectObject> githubDtos = Set.of(priorityField, stateField);

        when(gitHubClient.getIssueProjectItems("project123")).thenReturn(githubDtos);
        when(mapper.toEntity(priorityDto1, IssuePriority.class)).thenReturn(priorityEntity1);
        when(mapper.toEntity(priorityDto2, IssuePriority.class)).thenReturn(priorityEntity2);
        when(issuePriorityRepository.saveAll(anySet())).thenReturn(List.of(priorityEntity1, priorityEntity2));

        issueProjectItemsService.injectIssueProjectItems();

        verify(issuePriorityRepository)
                .saveAll(argThat((Set<IssuePriority> set) ->
                        set.size() == 2 && set.contains(priorityEntity1) && set.contains(priorityEntity2)));
        verify(issueStateRepository, never()).saveAll(anySet());
    }

    @Test
    public void injectIssueProjectItems_shouldOnlySaveStatesWhenPrioritiesAlreadyExist()
            throws GitHubClientException, IssueProjectItemInjectionException {
        when(issuePriorityRepository.count()).thenReturn(1L);
        when(issueStateRepository.count()).thenReturn(0L).thenReturn(0L);

        GitHubSingleSelectDTO.SingleSelectObject priorityField =
                new GitHubSingleSelectDTO.SingleSelectObject("Priority", List.of(priorityDto1, priorityDto2));
        GitHubSingleSelectDTO.SingleSelectObject stateField =
                new GitHubSingleSelectDTO.SingleSelectObject("Status", List.of(stateDto1, stateDto2));

        Set<GitHubSingleSelectDTO.SingleSelectObject> githubDtos = Set.of(priorityField, stateField);

        when(gitHubClient.getIssueProjectItems("project123")).thenReturn(githubDtos);
        when(mapper.toEntity(any(GitHubSingleSelectProjectItemDTO.class), eq(IssueState.class)))
                .thenAnswer(invocation -> {
                    GitHubSingleSelectProjectItemDTO dto = invocation.getArgument(0);
                    if (dto.id().equals("s1")) return stateEntity1;
                    if (dto.id().equals("s2")) return stateEntity2;
                    return null;
                });
        when(issueStateRepository.saveAll(anySet())).thenReturn(List.of(stateEntity1, stateEntity2));

        issueProjectItemsService.injectIssueProjectItems();

        verify(issuePriorityRepository, never()).saveAll(anySet());
        verify(issueStateRepository)
                .saveAll(argThat((Set<IssueState> set) ->
                        set.size() == 2 && set.contains(stateEntity1) && set.contains(stateEntity2)));
    }

    @Test
    public void injectIssueProjectItems_shouldSaveOnlyStatesFromCorrectField()
            throws GitHubClientException, IssueProjectItemInjectionException {
        when(issuePriorityRepository.count()).thenReturn(1L);
        when(issueStateRepository.count()).thenReturn(0L).thenReturn(0L);

        GitHubSingleSelectDTO.SingleSelectObject stateField =
                new GitHubSingleSelectDTO.SingleSelectObject("Status", List.of(stateDto1, stateDto2));

        Set<GitHubSingleSelectDTO.SingleSelectObject> githubDtos = Set.of(stateField);

        when(gitHubClient.getIssueProjectItems("project123")).thenReturn(githubDtos);
        when(mapper.toEntity(any(GitHubSingleSelectProjectItemDTO.class), eq(IssueState.class)))
                .thenAnswer(invocation -> {
                    GitHubSingleSelectProjectItemDTO dto = invocation.getArgument(0);
                    if (dto.id().equals("s1")) return stateEntity1;
                    if (dto.id().equals("s2")) return stateEntity2;
                    return null;
                });
        when(issueStateRepository.saveAll(anySet())).thenReturn(List.of(stateEntity1, stateEntity2));

        issueProjectItemsService.injectIssueProjectItems();

        verify(issueStateRepository)
                .saveAll(argThat((Set<IssueState> set) ->
                        set.size() == 2 && set.contains(stateEntity1) && set.contains(stateEntity2)));
    }

    @Test
    public void injectIssueProjectItems_shouldHandleWrongStateFieldName()
            throws GitHubClientException, IssueProjectItemInjectionException {
        when(issuePriorityRepository.count()).thenReturn(1L);
        when(issueStateRepository.count()).thenReturn(0L).thenReturn(0L);

        GitHubSingleSelectDTO.SingleSelectObject wrongStateField =
                new GitHubSingleSelectDTO.SingleSelectObject("State", List.of(stateDto1, stateDto2));

        Set<GitHubSingleSelectDTO.SingleSelectObject> githubDtos = Set.of(wrongStateField);

        when(gitHubClient.getIssueProjectItems("project123")).thenReturn(githubDtos);
        when(issueStateRepository.saveAll(anySet())).thenReturn(List.of());

        issueProjectItemsService.injectIssueProjectItems();
        verify(issueStateRepository).saveAll(argThat((Set<IssueState> set) -> set.isEmpty()));
    }

    @Test
    public void injectIssueProjectItems_shouldHandleNullStateOptions()
            throws GitHubClientException, IssueProjectItemInjectionException {
        when(issuePriorityRepository.count()).thenReturn(1L);
        when(issueStateRepository.count()).thenReturn(0L).thenReturn(0L);

        GitHubSingleSelectDTO.SingleSelectObject nullStateOptions =
                new GitHubSingleSelectDTO.SingleSelectObject("Status", null);

        Set<GitHubSingleSelectDTO.SingleSelectObject> githubDtos = Set.of(nullStateOptions);

        when(gitHubClient.getIssueProjectItems("project123")).thenReturn(githubDtos);
        when(issueStateRepository.saveAll(anySet())).thenReturn(List.of());

        issueProjectItemsService.injectIssueProjectItems();
        verify(issueStateRepository).saveAll(argThat((Set<IssueState> set) -> set.isEmpty()));
    }

    @Test
    public void injectIssueProjectItems_shouldHandleEmptyStateOptions()
            throws GitHubClientException, IssueProjectItemInjectionException {
        when(issuePriorityRepository.count()).thenReturn(1L);
        when(issueStateRepository.count()).thenReturn(0L).thenReturn(0L);

        GitHubSingleSelectDTO.SingleSelectObject emptyStateOptions =
                new GitHubSingleSelectDTO.SingleSelectObject("Status", List.of());

        Set<GitHubSingleSelectDTO.SingleSelectObject> githubDtos = Set.of(emptyStateOptions);

        when(gitHubClient.getIssueProjectItems("project123")).thenReturn(githubDtos);
        when(issueStateRepository.saveAll(anySet())).thenReturn(List.of());

        issueProjectItemsService.injectIssueProjectItems();
        verify(issueStateRepository).saveAll(argThat((Set<IssueState> set) -> set.isEmpty()));
    }

    @Test
    public void injectIssueProjectItems_shouldSaveOnlyPrioritiesFromCorrectField()
            throws GitHubClientException, IssueProjectItemInjectionException {
        when(issuePriorityRepository.count()).thenReturn(0L).thenReturn(0L);
        when(issueStateRepository.count()).thenReturn(1L);

        GitHubSingleSelectDTO.SingleSelectObject priorityField =
                new GitHubSingleSelectDTO.SingleSelectObject("Priority", List.of(priorityDto1, priorityDto2));

        Set<GitHubSingleSelectDTO.SingleSelectObject> githubDtos = Set.of(priorityField);

        when(gitHubClient.getIssueProjectItems("project123")).thenReturn(githubDtos);
        when(mapper.toEntity(priorityDto1, IssuePriority.class)).thenReturn(priorityEntity1);
        when(mapper.toEntity(priorityDto2, IssuePriority.class)).thenReturn(priorityEntity2);
        when(issuePriorityRepository.saveAll(anySet())).thenReturn(List.of(priorityEntity1, priorityEntity2));

        issueProjectItemsService.injectIssueProjectItems();

        verify(issuePriorityRepository)
                .saveAll(argThat((Set<IssuePriority> set) ->
                        set.size() == 2 && set.contains(priorityEntity1) && set.contains(priorityEntity2)));
    }

    @Test
    public void injectIssueProjectItems_shouldHandleWrongPriorityFieldName()
            throws GitHubClientException, IssueProjectItemInjectionException {
        when(issuePriorityRepository.count()).thenReturn(0L).thenReturn(0L);
        when(issueStateRepository.count()).thenReturn(1L);

        GitHubSingleSelectDTO.SingleSelectObject wrongPriorityField =
                new GitHubSingleSelectDTO.SingleSelectObject("Prio", List.of(priorityDto1, priorityDto2));

        Set<GitHubSingleSelectDTO.SingleSelectObject> githubDtos = Set.of(wrongPriorityField);

        when(gitHubClient.getIssueProjectItems("project123")).thenReturn(githubDtos);
        when(issuePriorityRepository.saveAll(anySet())).thenReturn(List.of());

        issueProjectItemsService.injectIssueProjectItems();
        verify(issuePriorityRepository).saveAll(argThat((Set<IssuePriority> set) -> set.isEmpty()));
    }

    @Test
    public void injectIssueProjectItems_shouldHandleNullPriorityOptions()
            throws GitHubClientException, IssueProjectItemInjectionException {
        when(issuePriorityRepository.count()).thenReturn(0L).thenReturn(0L);
        when(issueStateRepository.count()).thenReturn(1L);

        GitHubSingleSelectDTO.SingleSelectObject nullPriorityOptions =
                new GitHubSingleSelectDTO.SingleSelectObject("Priority", null);

        Set<GitHubSingleSelectDTO.SingleSelectObject> githubDtos = Set.of(nullPriorityOptions);

        when(gitHubClient.getIssueProjectItems("project123")).thenReturn(githubDtos);
        when(issuePriorityRepository.saveAll(anySet())).thenReturn(List.of());

        issueProjectItemsService.injectIssueProjectItems();
        verify(issuePriorityRepository).saveAll(argThat((Set<IssuePriority> set) -> set.isEmpty()));
    }

    @Test
    public void injectIssueProjectItems_shouldHandleEmptyPriorityOptions()
            throws GitHubClientException, IssueProjectItemInjectionException {
        when(issuePriorityRepository.count()).thenReturn(0L).thenReturn(0L);
        when(issueStateRepository.count()).thenReturn(1L);

        GitHubSingleSelectDTO.SingleSelectObject emptyPriorityOptions =
                new GitHubSingleSelectDTO.SingleSelectObject("Priority", List.of());

        Set<GitHubSingleSelectDTO.SingleSelectObject> githubDtos = Set.of(emptyPriorityOptions);

        when(gitHubClient.getIssueProjectItems("project123")).thenReturn(githubDtos);
        when(issuePriorityRepository.saveAll(anySet())).thenReturn(List.of());

        issueProjectItemsService.injectIssueProjectItems();
        verify(issuePriorityRepository).saveAll(argThat((Set<IssuePriority> set) -> set.isEmpty()));
    }
}
