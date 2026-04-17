package org.frankframework.insights.businessvalue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.issue.Issue;
import org.frankframework.insights.issue.IssueNotFoundException;
import org.frankframework.insights.issue.IssueRepository;
import org.frankframework.insights.issue.IssueResponse;
import org.frankframework.insights.release.Release;
import org.frankframework.insights.release.ReleaseNotFoundException;
import org.frankframework.insights.release.ReleaseRepository;
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
    private ReleaseRepository releaseRepository;

    @Mock
    private Mapper mapper;

    @InjectMocks
    private BusinessValueService businessValueService;

    private Release release1;
    private Release release2;
    private BusinessValue businessValue1;
    private BusinessValue businessValue2;
    private Issue issue1;
    private Issue issue2;
    private UUID businessValueId;

    @BeforeEach
    public void setup() {
        businessValueId = UUID.randomUUID();

        release1 = new Release();
        release1.setId("release-1");
        release1.setName("v1.0.0");

        release2 = new Release();
        release2.setId("release-2");
        release2.setName("v2.0.0");

        businessValue1 = new BusinessValue();
        businessValue1.setId(businessValueId);
        businessValue1.setTitle("Performance");
        businessValue1.setDescription("Performance improvements");
        businessValue1.setRelease(release1);
        businessValue1.setIssues(new HashSet<>());

        businessValue2 = new BusinessValue();
        businessValue2.setId(UUID.randomUUID());
        businessValue2.setTitle("Security");
        businessValue2.setDescription("Security enhancements");
        businessValue2.setRelease(release1);
        businessValue2.setIssues(new HashSet<>());

        issue1 = new Issue();
        issue1.setId("issue1");
        issue1.setNumber(1);
        issue1.setTitle("Test Issue 1");
        issue1.setBusinessValue(businessValue1);
        issue1.setSubIssues(new HashSet<>());

        Issue subIssue1 = new Issue();
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

    // ---- getBusinessValuesByReleaseId ----

    @Test
    public void getBusinessValuesByReleaseId_returnsBusinessValuesForRelease() {
        when(businessValueRepository.findByReleaseId("release-1")).thenReturn(List.of(businessValue1, businessValue2));

        List<BusinessValueResponse> result = businessValueService.getBusinessValuesByReleaseId("release-1");

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(businessValueRepository).findByReleaseId("release-1");
    }

    @Test
    public void getBusinessValuesByReleaseId_returnsEmptyListWhenNoBusinessValues() {
        when(businessValueRepository.findByReleaseId("release-empty")).thenReturn(Collections.emptyList());

        List<BusinessValueResponse> result = businessValueService.getBusinessValuesByReleaseId("release-empty");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ---- getBusinessValueById ----

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
        assertEquals("release-1", result.releaseId());
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

    // ---- createBusinessValue ----

    @Test
    public void createBusinessValue_createsSuccessfully()
            throws BusinessValueAlreadyExistsException, ReleaseNotFoundException {
        BusinessValueRequest request = new BusinessValueRequest("Performance", "Performance improvements", "release-1");

        when(releaseRepository.findById("release-1")).thenReturn(Optional.of(release1));
        when(businessValueRepository.findByTitleAndRelease("Performance", release1))
                .thenReturn(Optional.empty());
        when(businessValueRepository.save(any(BusinessValue.class))).thenReturn(businessValue1);

        BusinessValueResponse result = businessValueService.createBusinessValue(request);

        assertNotNull(result);
        assertEquals("Performance", result.title());
        assertEquals("release-1", result.releaseId());
        verify(businessValueRepository).save(any(BusinessValue.class));
    }

    @Test
    public void createBusinessValue_throwsExceptionWhenTitleAlreadyExistsInRelease() {
        BusinessValueRequest request = new BusinessValueRequest("Performance", "Performance improvements", "release-1");

        when(releaseRepository.findById("release-1")).thenReturn(Optional.of(release1));
        when(businessValueRepository.findByTitleAndRelease("Performance", release1))
                .thenReturn(Optional.of(businessValue1));

        assertThrows(
                BusinessValueAlreadyExistsException.class, () -> businessValueService.createBusinessValue(request));

        verify(businessValueRepository, never()).save(any());
    }

    @Test
    public void createBusinessValue_allowsSameTitleInDifferentReleases()
            throws BusinessValueAlreadyExistsException, ReleaseNotFoundException {
        BusinessValueRequest request = new BusinessValueRequest("Performance", "Performance improvements", "release-2");

        when(releaseRepository.findById("release-2")).thenReturn(Optional.of(release2));
        when(businessValueRepository.findByTitleAndRelease("Performance", release2))
                .thenReturn(Optional.empty());

        BusinessValue bvInRelease2 = new BusinessValue();
        bvInRelease2.setId(UUID.randomUUID());
        bvInRelease2.setTitle("Performance");
        bvInRelease2.setDescription("Performance improvements");
        bvInRelease2.setRelease(release2);
        bvInRelease2.setIssues(new HashSet<>());

        when(businessValueRepository.save(any(BusinessValue.class))).thenReturn(bvInRelease2);

        BusinessValueResponse result = businessValueService.createBusinessValue(request);

        assertNotNull(result);
        assertEquals("Performance", result.title());
        assertEquals("release-2", result.releaseId());
    }

    @Test
    public void createBusinessValue_throwsExceptionWhenReleaseNotFound() {
        BusinessValueRequest request =
                new BusinessValueRequest("Performance", "Performance improvements", "nonexistent");

        when(releaseRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ReleaseNotFoundException.class, () -> businessValueService.createBusinessValue(request));

        verify(businessValueRepository, never()).save(any());
    }

    // ---- updateBusinessValue ----

    @Test
    public void updateBusinessValue_updatesSuccessfully()
            throws BusinessValueNotFoundException, BusinessValueAlreadyExistsException {
        UpdateBusinessValueRequest request = new UpdateBusinessValueRequest("Performance", "Updated description");

        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.of(businessValue1));
        when(businessValueRepository.save(businessValue1)).thenReturn(businessValue1);

        BusinessValueResponse result = businessValueService.updateBusinessValue(businessValueId, request);

        assertNotNull(result);
        assertEquals("Updated description", businessValue1.getDescription());
        verify(businessValueRepository).save(businessValue1);
    }

    @Test
    public void updateBusinessValue_throwsExceptionWhenNotFound() {
        UpdateBusinessValueRequest request = new UpdateBusinessValueRequest("Performance", "Updated description");

        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.empty());

        assertThrows(
                BusinessValueNotFoundException.class,
                () -> businessValueService.updateBusinessValue(businessValueId, request));

        verify(businessValueRepository, never()).save(any());
    }

    @Test
    public void updateBusinessValue_throwsExceptionWhenTitleAlreadyExistsInSameRelease() {
        UpdateBusinessValueRequest request = new UpdateBusinessValueRequest("Security", "Updated description");

        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.of(businessValue1));
        when(businessValueRepository.findByTitleAndRelease("Security", release1))
                .thenReturn(Optional.of(businessValue2));

        assertThrows(
                BusinessValueAlreadyExistsException.class,
                () -> businessValueService.updateBusinessValue(businessValueId, request));

        verify(businessValueRepository, never()).save(any());
    }

    // ---- deleteBusinessValue ----

    @Test
    public void deleteBusinessValue_deletesSuccessfully() throws BusinessValueNotFoundException {
        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.of(businessValue1));

        businessValueService.deleteBusinessValue(businessValueId);

        verify(businessValueRepository).delete(businessValue1);
    }

    @Test
    public void deleteBusinessValue_throwsExceptionWhenNotFound() {
        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.empty());

        assertThrows(
                BusinessValueNotFoundException.class, () -> businessValueService.deleteBusinessValue(businessValueId));

        verify(businessValueRepository, never()).delete(any());
    }

    @Test
    public void deleteBusinessValue_disconnectsIssuesBeforeDeleting() throws BusinessValueNotFoundException {
        Set<Issue> issues = new HashSet<>();
        issues.add(issue1);
        businessValue1.setIssues(issues);
        issue1.setBusinessValue(businessValue1);

        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.of(businessValue1));

        businessValueService.deleteBusinessValue(businessValueId);

        verify(issueRepository).saveAll(anySet());
        verify(businessValueRepository).delete(businessValue1);
        assertNull(issue1.getBusinessValue());
    }

    // ---- replaceIssueConnections ----

    @Test
    public void replaceIssueConnections_disconnectsOldAndConnectsNew()
            throws BusinessValueNotFoundException, IssueNotFoundException {
        Set<String> newIssueIds = Set.of("issue2");
        ConnectIssuesRequest request = new ConnectIssuesRequest(newIssueIds);

        Set<Issue> mutableIssues = new HashSet<>();
        mutableIssues.add(issue1);
        businessValue1.setIssues(mutableIssues);
        issue1.setBusinessValue(businessValue1);

        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.of(businessValue1));
        when(issueRepository.findById("issue2")).thenReturn(Optional.of(issue2));
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> List.copyOf(inv.getArgument(0)));

        BusinessValueResponse result = businessValueService.replaceIssueConnections(businessValueId, request);

        assertNotNull(result);
        assertNull(issue1.getBusinessValue());
        assertEquals(businessValue1, issue2.getBusinessValue());
        verify(issueRepository, times(2)).saveAll(anySet());
    }

    @Test
    public void replaceIssueConnections_connectsExactlyTheProvidedIssues()
            throws BusinessValueNotFoundException, IssueNotFoundException {
        Set<String> newIssueIds = Set.of("issue2");
        ConnectIssuesRequest request = new ConnectIssuesRequest(newIssueIds);

        businessValue1.setIssues(new HashSet<>());

        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.of(businessValue1));
        when(issueRepository.findById("issue2")).thenReturn(Optional.of(issue2));
        when(issueRepository.saveAll(anySet())).thenAnswer(inv -> List.copyOf(inv.getArgument(0)));

        BusinessValueResponse result = businessValueService.replaceIssueConnections(businessValueId, request);

        assertNotNull(result);
        assertEquals(businessValue1, issue2.getBusinessValue());
        // sub-issues of issue2 are NOT auto-connected
        assertEquals(1, businessValue1.getIssues().size());
        verify(issueRepository, times(1)).saveAll(anySet());
    }

    @Test
    public void replaceIssueConnections_throwsExceptionWhenIssueNotFound() {
        Set<String> newIssueIds = Set.of("nonexistent");
        ConnectIssuesRequest request = new ConnectIssuesRequest(newIssueIds);

        businessValue1.setIssues(new HashSet<>());

        when(businessValueRepository.findById(businessValueId)).thenReturn(Optional.of(businessValue1));
        when(issueRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(
                IssueNotFoundException.class,
                () -> businessValueService.replaceIssueConnections(businessValueId, request));
    }

    // ---- duplicateBusinessValues ----

    @Test
    public void duplicateBusinessValues_duplicatesAllFromSourceRelease() throws ReleaseNotFoundException {
        DuplicateBusinessValuesRequest request = new DuplicateBusinessValuesRequest("release-1");

        when(releaseRepository.findById("release-2")).thenReturn(Optional.of(release2));
        when(businessValueRepository.findByReleaseId("release-1")).thenReturn(List.of(businessValue1, businessValue2));
        when(businessValueRepository.findByReleaseId("release-2")).thenReturn(Collections.emptyList());
        when(businessValueRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<BusinessValueResponse> result = businessValueService.duplicateBusinessValues("release-2", request);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(businessValueRepository).saveAll(anyList());
    }

    @Test
    public void duplicateBusinessValues_skipsExistingTitlesInTargetRelease() throws ReleaseNotFoundException {
        DuplicateBusinessValuesRequest request = new DuplicateBusinessValuesRequest("release-1");

        BusinessValue existingInTarget = new BusinessValue();
        existingInTarget.setId(UUID.randomUUID());
        existingInTarget.setTitle("Performance");
        existingInTarget.setDescription("Already exists");
        existingInTarget.setRelease(release2);
        existingInTarget.setIssues(new HashSet<>());

        when(releaseRepository.findById("release-2")).thenReturn(Optional.of(release2));
        when(businessValueRepository.findByReleaseId("release-1")).thenReturn(List.of(businessValue1, businessValue2));
        when(businessValueRepository.findByReleaseId("release-2")).thenReturn(List.of(existingInTarget));
        when(businessValueRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<BusinessValueResponse> result = businessValueService.duplicateBusinessValues("release-2", request);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Security", result.get(0).title());
    }

    @Test
    public void duplicateBusinessValues_returnsEmptyWhenSourceHasNoBusinessValues() throws ReleaseNotFoundException {
        DuplicateBusinessValuesRequest request = new DuplicateBusinessValuesRequest("release-empty");

        when(releaseRepository.findById("release-2")).thenReturn(Optional.of(release2));
        when(businessValueRepository.findByReleaseId("release-empty")).thenReturn(Collections.emptyList());
        when(businessValueRepository.findByReleaseId("release-2")).thenReturn(Collections.emptyList());
        when(businessValueRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        List<BusinessValueResponse> result = businessValueService.duplicateBusinessValues("release-2", request);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void duplicateBusinessValues_throwsExceptionWhenTargetReleaseNotFound() {
        DuplicateBusinessValuesRequest request = new DuplicateBusinessValuesRequest("release-1");

        when(releaseRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(
                ReleaseNotFoundException.class,
                () -> businessValueService.duplicateBusinessValues("nonexistent", request));

        verify(businessValueRepository, never()).saveAll(anyList());
    }
}
