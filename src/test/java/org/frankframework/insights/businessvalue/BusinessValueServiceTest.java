package org.frankframework.insights.businessvalue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.issue.Issue;
import org.frankframework.insights.issue.IssueNotFoundException;
import org.frankframework.insights.issue.IssueRepository;
import org.frankframework.insights.issue.IssueResponse;
import org.frankframework.insights.issue.IssueService;
import org.frankframework.insights.release.ReleaseNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BusinessValueServiceTest {

    @Mock
    private BusinessValueRepository businessValueRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private IssueService issueService;

    @Mock
    private Mapper mapper;

    @InjectMocks
    private BusinessValueService businessValueService;

    private BusinessValue businessValue1;
    private BusinessValue businessValue2;
    private Issue issue1;
    private Issue issue2;
    private Issue subIssue1;
    private UUID businessValueId;

    @BeforeEach
    public void setup() {
        businessValueId = UUID.randomUUID();

        businessValue1 = new BusinessValue();
        businessValue1.setId(businessValueId);
        businessValue1.setTitle("Performance");
        businessValue1.setDescription("Performance improvements");
        businessValue1.setIssues(new HashSet<>());

        businessValue2 = new BusinessValue();
        businessValue2.setId(UUID.randomUUID());
        businessValue2.setTitle("Security");
        businessValue2.setDescription("Security enhancements");
        businessValue2.setIssues(new HashSet<>());

        issue1 = new Issue();
        issue1.setId("issue1");
        issue1.setNumber(1);
        issue1.setTitle("Test Issue 1");
        issue1.setBusinessValue(businessValue1);
        issue1.setSubIssues(new HashSet<>());

        subIssue1 = new Issue();
        subIssue1.setId("subIssue1");
        subIssue1.setNumber(2);
        subIssue1.setTitle("Sub Issue 1");
        subIssue1.setSubIssues(new HashSet<>());

        issue2 = new Issue();
        issue2.setId("issue2");
        issue2.setNumber(3);
        issue2.setTitle("Test Issue 2");
        issue2.setSubIssues(Set.of(subIssue1));
    }

    @Test
    public void getBusinessValuesByReleaseId_returnsBusinessValues() throws ReleaseNotFoundException {
        String releaseId = "rel123";
        Set<Issue> rootIssues = Set.of(issue1);

        when(issueService.getRootIssuesByReleaseId(releaseId)).thenReturn(rootIssues);
        when(businessValueRepository.findByTitle("Performance")).thenReturn(Optional.of(businessValue1));

        Set<BusinessValueResponse> result = businessValueService.getBusinessValuesByReleaseId(releaseId);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(issueService).getRootIssuesByReleaseId(releaseId);
        verify(businessValueRepository).findByTitle("Performance");
    }

    @Test
    public void getBusinessValuesByReleaseId_handlesNullBusinessValues() throws ReleaseNotFoundException {
        String releaseId = "rel123";
        Issue issueWithoutBV = new Issue();
        issueWithoutBV.setId("issue3");
        issueWithoutBV.setBusinessValue(null);
        Set<Issue> rootIssues = Set.of(issueWithoutBV);

        when(issueService.getRootIssuesByReleaseId(releaseId)).thenReturn(rootIssues);

        Set<BusinessValueResponse> result = businessValueService.getBusinessValuesByReleaseId(releaseId);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void createBusinessValue_createsSuccessfully() throws BusinessValueAlreadyExistsException {
        BusinessValueRequest request = new BusinessValueRequest("Performance", "Performance improvements");

        when(businessValueRepository.findByTitle("Performance")).thenReturn(Optional.empty());
        when(mapper.toEntity(request, BusinessValue.class)).thenReturn(businessValue1);
        when(businessValueRepository.save(businessValue1)).thenReturn(businessValue1);

        BusinessValueResponse result = businessValueService.createBusinessValue(request);

        assertNotNull(result);
        verify(businessValueRepository).findByTitle("Performance");
        verify(businessValueRepository).save(businessValue1);
    }

    @Test
    public void createBusinessValue_throwsExceptionWhenAlreadyExists() {
        BusinessValueRequest request = new BusinessValueRequest("Performance", "Performance improvements");

        when(businessValueRepository.findByTitle("Performance")).thenReturn(Optional.of(businessValue1));

        assertThrows(
                BusinessValueAlreadyExistsException.class, () -> businessValueService.createBusinessValue(request));

        verify(businessValueRepository).findByTitle("Performance");
        verify(businessValueRepository, never()).save(any());
    }

    @Test
    public void getBusinessValueById_returnsBusinessValueWithIssues() throws BusinessValueNotFoundException {
        Set<Issue> issues = Set.of(issue1);
        businessValue1.setIssues(issues);

        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.of(businessValue1));
        when(mapper.toDTO(any(Issue.class), eq(IssueResponse.class))).thenReturn(new IssueResponse());

        BusinessValueResponse result = businessValueService.getBusinessValueById(businessValueId);

        assertNotNull(result);
        assertEquals(businessValueId, result.id());
        assertEquals("Performance", result.title());
        assertEquals(1, result.issues().size());
        verify(businessValueRepository).findById(businessValueId);
    }

    @Test
    public void getBusinessValueById_throwsExceptionWhenNotFound() {
        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.empty());

        assertThrows(
                BusinessValueNotFoundException.class, () -> businessValueService.getBusinessValueById(businessValueId));

        verify(businessValueRepository).findById(businessValueId);
    }

    @Test
    public void getAllBusinessValues_returnsAllBusinessValues() {
        List<BusinessValue> businessValues = List.of(businessValue1, businessValue2);

        when(businessValueRepository.findAll()).thenReturn(businessValues);

        Set<BusinessValueResponse> result = businessValueService.getAllBusinessValues();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(businessValueRepository).findAll();
    }

    @Test
    public void getAllBusinessValues_returnsEmptySetWhenNoneExist() {
        when(businessValueRepository.findAll()).thenReturn(Collections.emptyList());

        Set<BusinessValueResponse> result = businessValueService.getAllBusinessValues();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(businessValueRepository).findAll();
    }

    @Test
    public void connectIssuesToBusinessValue_connectsIssuesSuccessfully()
            throws BusinessValueNotFoundException, IssueNotFoundException {
        Set<String> issueIds = Set.of("issue1");
        ConnectIssuesRequest request = new ConnectIssuesRequest(issueIds);

        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.of(businessValue1));
        when(issueRepository.findById("issue1")).thenReturn(Optional.of(issue1));
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> {
            Set<Issue> issues = inv.getArgument(0);
            return List.copyOf(issues);
        });
        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.of(businessValue1));

        BusinessValueResponse result = businessValueService.connectIssuesToBusinessValue(businessValueId, request);

        assertNotNull(result);
        verify(businessValueRepository, atLeast(2)).findById(businessValueId);
        verify(issueRepository).saveAll(anySet());
    }

    @Test
    public void connectIssuesToBusinessValue_connectsSubIssuesAutomatically()
            throws BusinessValueNotFoundException, IssueNotFoundException {
        Set<String> issueIds = Set.of("issue2");
        ConnectIssuesRequest request = new ConnectIssuesRequest(issueIds);

        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.of(businessValue1));
        when(issueRepository.findById("issue2")).thenReturn(Optional.of(issue2));
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> {
            Set<Issue> savedIssues = inv.getArgument(0);
            assertEquals(2, savedIssues.size());
            return List.copyOf(savedIssues);
        });
        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.of(businessValue1));

        BusinessValueResponse result = businessValueService.connectIssuesToBusinessValue(businessValueId, request);

        assertNotNull(result);
        verify(issueRepository).saveAll(anySet());
    }

    @Test
    public void connectIssuesToBusinessValue_throwsExceptionWhenBusinessValueNotFound() {
        Set<String> issueIds = Set.of("issue1");
        ConnectIssuesRequest request = new ConnectIssuesRequest(issueIds);

        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.empty());

        assertThrows(
                BusinessValueNotFoundException.class,
                () -> businessValueService.connectIssuesToBusinessValue(businessValueId, request));

        verify(businessValueRepository).findById(businessValueId);
        verify(issueRepository, never()).saveAll(anySet());
    }

    @Test
    public void connectIssuesToBusinessValue_throwsExceptionWhenIssueNotFound() {
        Set<String> issueIds = Set.of("issue1", "nonexistent");
        ConnectIssuesRequest request = new ConnectIssuesRequest(issueIds);

        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.of(businessValue1));
        when(issueRepository.findById("issue1")).thenReturn(Optional.of(issue1));
        when(issueRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(
                IssueNotFoundException.class,
                () -> businessValueService.connectIssuesToBusinessValue(businessValueId, request));

        verify(businessValueRepository).findById(businessValueId);
        verify(issueRepository, never()).saveAll(anySet());
    }

    @Test
    public void updateBusinessValue_updatesSuccessfully()
            throws BusinessValueNotFoundException, BusinessValueAlreadyExistsException {
        BusinessValueRequest request = new BusinessValueRequest("Performance", "Updated description");

        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.of(businessValue1));
        when(businessValueRepository.save(businessValue1)).thenReturn(businessValue1);

        BusinessValueResponse result = businessValueService.updateBusinessValue(businessValueId, request);

        assertNotNull(result);
        assertEquals("Updated description", businessValue1.getDescription());
        verify(businessValueRepository).findById(businessValueId);
        verify(businessValueRepository).save(businessValue1);
    }

    @Test
    public void updateBusinessValue_throwsExceptionWhenNotFound() {
        BusinessValueRequest request = new BusinessValueRequest("Performance", "Updated description");

        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.empty());

        assertThrows(
                BusinessValueNotFoundException.class,
                () -> businessValueService.updateBusinessValue(businessValueId, request));

        verify(businessValueRepository).findById(businessValueId);
        verify(businessValueRepository, never()).save(any());
    }

    @Test
    public void updateBusinessValue_throwsExceptionWhenNameAlreadyExists() {
        BusinessValueRequest request = new BusinessValueRequest("Security", "Updated description");

        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.of(businessValue1));
        when(businessValueRepository.findByTitle("Security")).thenReturn(Optional.of(businessValue2));

        assertThrows(
                BusinessValueAlreadyExistsException.class,
                () -> businessValueService.updateBusinessValue(businessValueId, request));

        verify(businessValueRepository).findById(businessValueId);
        verify(businessValueRepository, never()).save(any());
    }

    @Test
    public void disconnectIssuesFromBusinessValue_disconnectsSuccessfully() throws BusinessValueNotFoundException {
        Set<String> issueIds = Set.of("issue1");
        issue1.setBusinessValue(businessValue1);

        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.of(businessValue1));
        when(issueRepository.findById("issue1")).thenReturn(Optional.of(issue1));
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> {
            Set<Issue> issues = inv.getArgument(0);
            return List.copyOf(issues);
        });
        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.of(businessValue1));

        BusinessValueResponse result =
                businessValueService.disconnectIssuesFromBusinessValue(businessValueId, issueIds);

        assertNotNull(result);
        assertNull(issue1.getBusinessValue());
        verify(issueRepository).saveAll(anySet());
    }

    @Test
    public void disconnectIssuesFromBusinessValue_disconnectsSubIssuesAutomatically()
            throws BusinessValueNotFoundException {
        Set<String> issueIds = Set.of("issue2");
        issue2.setBusinessValue(businessValue1);
        subIssue1.setBusinessValue(businessValue1);

        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.of(businessValue1));
        when(issueRepository.findById("issue2")).thenReturn(Optional.of(issue2));
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> {
            Set<Issue> savedIssues = inv.getArgument(0);
            assertEquals(2, savedIssues.size());
            return List.copyOf(savedIssues);
        });
        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.of(businessValue1));

        BusinessValueResponse result =
                businessValueService.disconnectIssuesFromBusinessValue(businessValueId, issueIds);

        assertNotNull(result);
        verify(issueRepository).saveAll(anySet());
    }

    @Test
    public void replaceIssueConnections_replacesSuccessfully()
            throws BusinessValueNotFoundException, IssueNotFoundException {
        Set<String> newIssueIds = Set.of("issue2");
        ConnectIssuesRequest request = new ConnectIssuesRequest(newIssueIds);

        businessValue1.setIssues(Set.of(issue1));
        issue1.setBusinessValue(businessValue1);

        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.of(businessValue1));
        when(issueRepository.findById("issue1")).thenReturn(Optional.of(issue1));
        when(issueRepository.findById("issue2")).thenReturn(Optional.of(issue2));
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> {
            Set<Issue> issues = inv.getArgument(0);
            return List.copyOf(issues);
        });

        BusinessValueResponse result = businessValueService.replaceIssueConnections(businessValueId, request);

        assertNotNull(result);
        verify(issueRepository, atLeast(2)).saveAll(anySet());
    }

    @Test
    public void replaceIssueConnections_handlesEmptyCurrentIssues()
            throws BusinessValueNotFoundException, IssueNotFoundException {
        Set<String> newIssueIds = Set.of("issue1");
        ConnectIssuesRequest request = new ConnectIssuesRequest(newIssueIds);

        businessValue1.setIssues(new HashSet<>());

        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.of(businessValue1));
        when(issueRepository.findById("issue1")).thenReturn(Optional.of(issue1));
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> {
            Set<Issue> issues = inv.getArgument(0);
            return List.copyOf(issues);
        });

        BusinessValueResponse result = businessValueService.replaceIssueConnections(businessValueId, request);

        assertNotNull(result);
        verify(issueRepository).saveAll(anySet());
    }
}
