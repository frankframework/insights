package org.frankframework.webapp.issue;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.Set;
import org.frankframework.webapp.milestone.MilestoneNotFoundException;
import org.frankframework.webapp.release.ReleaseNotFoundException;
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
    public void getFutureEpicIssues_returnsOkWithIssues() throws Exception {
        IssueResponse epic1 = new IssueResponse();
        epic1.setId("epic1");
        Set<IssueResponse> futureIssues = Set.of(epic1);
        when(issueService.getFutureEpicIssues()).thenReturn(futureIssues);

        mockMvc.perform(get("/api/issues/future"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("epic1"));
    }

    @Test
    public void getFutureEpicIssues_returnsEmptySet() throws Exception {
        when(issueService.getFutureEpicIssues()).thenReturn(Collections.emptySet());

        mockMvc.perform(get("/api/issues/future"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
