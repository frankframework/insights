package org.frankframework.insights.issue;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Set;
import org.frankframework.insights.milestone.MilestoneNotFoundException;
import org.frankframework.insights.release.ReleaseNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IssueController.class)
public class IssueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IssueService issueService;

    @TestConfiguration
    public static class MockConfig {
        @Bean
        public IssueService issueService() {
            return mock(IssueService.class);
        }
    }

    @BeforeEach
    public void resetMocks() {
        reset(issueService);
    }

    @Test
    public void getIssuesByReleaseId_returnsOkWithIssues() throws Exception {
        IssueResponse response1 = new IssueResponse();
        IssueResponse response2 = new IssueResponse();
        Set<IssueResponse> issues = Set.of(response1, response2);

        when(issueService.getIssuesByReleaseId("rel1")).thenReturn(issues);

        mockMvc.perform(get("/api/issues/release/rel1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    public void getIssuesByReleaseId_returnsEmptySet() throws Exception {
        when(issueService.getIssuesByReleaseId("rel1")).thenReturn(Collections.emptySet());

        mockMvc.perform(get("/api/issues/release/rel1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void getIssuesByReleaseId_returnsNull_treatedAsEmptySet() throws Exception {
        when(issueService.getIssuesByReleaseId("rel1")).thenReturn(null);

        mockMvc.perform(get("/api/issues/release/rel1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void getIssuesByReleaseId_throwsReleaseNotFoundException_returns404() throws Exception {
        when(issueService.getIssuesByReleaseId("relX")).thenThrow(new ReleaseNotFoundException("Not found", null));

        mockMvc.perform(get("/api/issues/release/relX")).andExpect(status().isNotFound());
    }

    @Test
    public void getIssuesByMilestoneId_returnsOkWithIssues() throws Exception {
        IssueResponse response1 = new IssueResponse();
        Set<IssueResponse> issues = Set.of(response1);

        when(issueService.getIssuesByMilestoneId("mil1")).thenReturn(issues);

        mockMvc.perform(get("/api/issues/milestone/mil1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    public void getIssuesByMilestoneId_returnsEmptySet() throws Exception {
        when(issueService.getIssuesByMilestoneId("mil1")).thenReturn(Collections.emptySet());

        mockMvc.perform(get("/api/issues/milestone/mil1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void getIssuesByMilestoneId_returnsNull_treatedAsEmptySet() throws Exception {
        when(issueService.getIssuesByMilestoneId("mil1")).thenReturn(null);

        mockMvc.perform(get("/api/issues/milestone/mil1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void getIssuesByMilestoneId_throwsMilestoneNotFoundException_returns404() throws Exception {
        when(issueService.getIssuesByMilestoneId("milX")).thenThrow(new MilestoneNotFoundException("Not found", null));

        mockMvc.perform(get("/api/issues/milestone/milX")).andExpect(status().isNotFound());
    }

    @Test
    public void getIssuesByTimespan_returnsOkWithIssues() throws Exception {
        OffsetDateTime start = OffsetDateTime.parse("2023-01-01T00:00:00Z");
        OffsetDateTime end = OffsetDateTime.parse("2023-12-31T23:59:59Z");

        IssueResponse resp = new IssueResponse();
        Set<IssueResponse> issues = Set.of(resp);

        when(issueService.getIssuesByTimespan(start, end)).thenReturn(issues);

        mockMvc.perform(get("/api/issues/timespan")
                        .param("startDate", start.toString())
                        .param("endDate", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    public void getIssuesByTimespan_returnsEmptySet() throws Exception {
        OffsetDateTime start = OffsetDateTime.parse("2023-01-01T00:00:00Z");
        OffsetDateTime end = OffsetDateTime.parse("2023-12-31T23:59:59Z");

        when(issueService.getIssuesByTimespan(start, end)).thenReturn(Collections.emptySet());

        mockMvc.perform(get("/api/issues/timespan")
                        .param("startDate", start.toString())
                        .param("endDate", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void getIssuesByTimespan_returnsNull_treatedAsEmptySet() throws Exception {
        OffsetDateTime start = OffsetDateTime.parse("2023-01-01T00:00:00Z");
        OffsetDateTime end = OffsetDateTime.parse("2023-12-31T23:59:59Z");

        when(issueService.getIssuesByTimespan(start, end)).thenReturn(null);

        mockMvc.perform(get("/api/issues/timespan")
                        .param("startDate", start.toString())
                        .param("endDate", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void getIssuesByTimespan_missingParam_returnsBadRequest() throws Exception {
        OffsetDateTime start = OffsetDateTime.parse("2023-01-01T00:00:00Z");

        mockMvc.perform(get("/api/issues/timespan").param("startDate", start.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getIssuesByTimespan_invalidParam_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/issues/timespan")
                        .param("startDate", "not-a-date")
                        .param("endDate", "2024-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }
}
