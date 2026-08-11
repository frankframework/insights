package org.frankframework.insights.release;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.*;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.common.mapper.MappingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReleaseQueryServiceTest {
    @Mock
    private Mapper mapper;

    @Mock
    private ReleaseRepository releaseRepository;

    private ReleaseQueryService releaseQueryService;

    private Release rel1;

    private Release rel2;

    private Branch masterBranch;

    @BeforeEach
    public void setUp() {
        masterBranch = new Branch();
        masterBranch.setId(UUID.randomUUID().toString());
        masterBranch.setName("master");

        rel1 = new Release();
        rel1.setId("id1");
        rel1.setTagName("v1.0");
        rel1.setPublishedAt(OffsetDateTime.now().minusDays(10));
        rel1.setBranch(masterBranch);

        rel2 = new Release();
        rel2.setId("id2");
        rel2.setTagName("v1.1");
        rel2.setPublishedAt(OffsetDateTime.now());
        rel2.setBranch(masterBranch);

        releaseQueryService = new ReleaseQueryService(mapper, releaseRepository);
    }

    @Test
    public void getAllReleases_returnsAll() {
        ReleaseResponse resp1 = mock(ReleaseResponse.class);
        ReleaseResponse resp2 = mock(ReleaseResponse.class);
        when(releaseRepository.findAll()).thenReturn(List.of(rel1, rel2));
        when(mapper.toDTO(rel1, ReleaseResponse.class)).thenReturn(resp1);
        when(mapper.toDTO(rel2, ReleaseResponse.class)).thenReturn(resp2);

        Set<ReleaseResponse> result = releaseQueryService.getAllReleases();
        assertEquals(Set.of(resp1, resp2), result);
    }

    @Test
    public void checkIfReleaseExists_returnsRelease() throws ReleaseNotFoundException {
        when(releaseRepository.findById("id1")).thenReturn(Optional.of(rel1));
        Release found = releaseQueryService.checkIfReleaseExists("id1");
        assertEquals(rel1, found);
    }

    @Test
    public void checkIfReleaseExists_throwsIfNotFound() {
        when(releaseRepository.findById("id2")).thenReturn(Optional.empty());
        assertThrows(ReleaseNotFoundException.class, () -> releaseQueryService.checkIfReleaseExists("id2"));
    }

    @Test
    public void getReleaseById_returnsReleaseResponse() throws ReleaseNotFoundException {
        ReleaseResponse mockResponse = mock(ReleaseResponse.class);
        when(releaseRepository.findById("release-1")).thenReturn(Optional.of(rel1));
        when(mapper.toDTO(rel1, ReleaseResponse.class)).thenReturn(mockResponse);

        ReleaseResponse result = releaseQueryService.getReleaseById("release-1");

        assertEquals(mockResponse, result);
        verify(releaseRepository).findById("release-1");
        verify(mapper).toDTO(rel1, ReleaseResponse.class);
    }

    @Test
    public void getReleaseById_throwsReleaseNotFoundException_whenReleaseNotFound() throws MappingException {
        when(releaseRepository.findById("nonexistent-id")).thenReturn(Optional.empty());

        ReleaseNotFoundException exception = assertThrows(
                ReleaseNotFoundException.class, () -> releaseQueryService.getReleaseById("nonexistent-id"));

        assertEquals("Release with ID [nonexistent-id] not found.", exception.getMessage());
        verify(releaseRepository).findById("nonexistent-id");
        verify(mapper, never()).toDTO(any(), eq(ReleaseResponse.class));
    }

    @Test
    public void getReleaseById_handlesEmptyString() {
        when(releaseRepository.findById("")).thenReturn(Optional.empty());

        assertThrows(ReleaseNotFoundException.class, () -> releaseQueryService.getReleaseById(""));
        verify(releaseRepository).findById("");
    }
}
