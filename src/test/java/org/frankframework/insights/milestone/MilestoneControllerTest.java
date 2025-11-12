package org.frankframework.insights.milestone;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.Set;
import org.frankframework.insights.common.configuration.TestSecurityConfig;
import org.frankframework.insights.common.mapper.MappingException;
import org.frankframework.insights.github.graphql.GitHubPropertyState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = MilestoneController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class})
@Import(TestSecurityConfig.class)
public class MilestoneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MilestoneService milestoneService;

    @TestConfiguration
    public static class MockConfig {
        @Bean
        public MilestoneService milestoneService() {
            return mock(MilestoneService.class);
        }
    }

    @BeforeEach
    public void resetMocks() {
        reset(milestoneService);
    }

    @Test
    public void getAllMilestones_returnsMilestones() throws Exception {
        MilestoneResponse m1 =
                new MilestoneResponse("id1", 1, "v1.0", "https//example.com", GitHubPropertyState.OPEN, null, 0, 0);
        MilestoneResponse m2 =
                new MilestoneResponse("id2", 2, "v2.0", "https//example.com", GitHubPropertyState.OPEN, null, 0, 0);
        Set<MilestoneResponse> milestones = Set.of(m1, m2);

        when(milestoneService.getAllMilestones()).thenReturn(milestones);

        mockMvc.perform(get("/api/milestones"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    public void getAllMilestones_returnsEmptySet() throws Exception {
        when(milestoneService.getAllMilestones()).thenReturn(Collections.emptySet());

        mockMvc.perform(get("/api/milestones"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void getAllMilestones_serviceReturnsNull_treatedAsEmptySet() throws Exception {
        when(milestoneService.getAllMilestones()).thenReturn(null);

        mockMvc.perform(get("/api/milestones"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void getAllMilestones_serviceThrowsMappingException_returns400() throws Exception {
        when(milestoneService.getAllMilestones()).thenThrow(new MappingException("fail", null));

        mockMvc.perform(get("/api/milestones")).andExpect(status().isBadRequest());
    }
}
