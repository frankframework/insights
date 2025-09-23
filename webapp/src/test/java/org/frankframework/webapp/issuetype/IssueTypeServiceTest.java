package org.frankframework.webapp.issuetype;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.frankframework.webapp.common.mapper.Mapper;
import org.frankframework.webapp.github.GitHubClient;
import org.frankframework.webapp.github.GitHubClientException;
import org.frankframework.webapp.github.GitHubRepositoryStatisticsDTO;
import org.frankframework.webapp.github.GitHubRepositoryStatisticsService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IssueTypeServiceTest {

    @Mock
    GitHubRepositoryStatisticsService statisticsService;

    @Mock
    IssueTypeRepository issueTypeRepository;

    @Mock
    GitHubClient gitHubClient;

    @Mock
    Mapper mapper;

    @InjectMocks
    IssueTypeService issueTypeService;

    @Mock
    GitHubRepositoryStatisticsDTO statsDTO;

    private IssueType type1, type2;
    private IssueTypeDTO dto1, dto2;

    @BeforeEach
    void setup() {
        type1 = new IssueType();
        type1.setId("it1");
        type1.setName("Type 1");

        type2 = new IssueType();
        type2.setId("it2");
        type2.setName("Type 2");

        dto1 = new IssueTypeDTO("it1", "Type 1", null, null);
        dto2 = new IssueTypeDTO("it2", "Type 2", null, null);
    }

    @Test
    void injectIssueTypes_shouldSkipIfCountsEqual() throws Exception {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statsDTO);
        when(statsDTO.getGitHubIssueTypeCount()).thenReturn(3);
        when(issueTypeRepository.count()).thenReturn(3L);

        issueTypeService.injectIssueTypes();

        verify(gitHubClient, never()).getIssueTypes();
        verify(issueTypeRepository, never()).saveAll(anySet());
    }

    @Test
    void injectIssueTypes_shouldSaveAllIssueTypes() throws Exception {
        Set<IssueTypeDTO> dtos = Set.of(dto1, dto2);
        Set<IssueType> entities = Set.of(type1, type2);
        List<IssueType> saved = List.of(type1, type2);

        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statsDTO);
        when(statsDTO.getGitHubIssueTypeCount()).thenReturn(2);
        when(issueTypeRepository.count()).thenReturn(1L);

        when(gitHubClient.getIssueTypes()).thenReturn(dtos);
        when(mapper.toEntity(dtos, IssueType.class)).thenReturn(entities);
        when(issueTypeRepository.saveAll(entities)).thenReturn(saved);

        issueTypeService.injectIssueTypes();

        verify(issueTypeRepository).saveAll(entities);
    }

    @Test
    void injectIssueTypes_shouldThrowOnException() throws Exception {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statsDTO);
        when(statsDTO.getGitHubIssueTypeCount()).thenReturn(2);
        when(issueTypeRepository.count()).thenReturn(1L);

        when(gitHubClient.getIssueTypes()).thenThrow(new GitHubClientException("fail", null));

        assertThrows(IssueTypeInjectionException.class, () -> issueTypeService.injectIssueTypes());
    }

    @Test
    void getAllIssueTypesMap_shouldReturnCorrectMap() {
        when(issueTypeRepository.findAll()).thenReturn(List.of(type1, type2));
        Map<String, IssueType> result = issueTypeService.getAllIssueTypesMap();
        assertEquals(2, result.size());
        assertEquals(type1, result.get("it1"));
        assertEquals(type2, result.get("it2"));
    }

    @Test
    void injectIssueTypes_shouldSaveEmptyListIfNoDTOSFromGitHub() throws Exception {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statsDTO);
        when(statsDTO.getGitHubIssueTypeCount()).thenReturn(2);
        when(issueTypeRepository.count()).thenReturn(1L);

        when(gitHubClient.getIssueTypes()).thenReturn(Collections.emptySet());
        when(mapper.toEntity(Collections.emptySet(), IssueType.class)).thenReturn(Collections.emptySet());
        when(issueTypeRepository.saveAll(Collections.emptySet())).thenReturn(Collections.emptyList());

        issueTypeService.injectIssueTypes();
        verify(issueTypeRepository).saveAll(Collections.emptySet());
    }

    @Test
    void injectIssueTypes_shouldPropagateMapperExceptionAsInjectionException() throws Exception {
        when(statisticsService.getGitHubRepositoryStatisticsDTO()).thenReturn(statsDTO);
        when(statsDTO.getGitHubIssueTypeCount()).thenReturn(2);
        when(issueTypeRepository.count()).thenReturn(1L);

        Set<IssueTypeDTO> dtos = Set.of(dto1);
        when(gitHubClient.getIssueTypes()).thenReturn(dtos);
        when(mapper.toEntity(dtos, IssueType.class)).thenThrow(new RuntimeException("Mapping failed"));

        assertThrows(IssueTypeInjectionException.class, () -> issueTypeService.injectIssueTypes());
    }
}
