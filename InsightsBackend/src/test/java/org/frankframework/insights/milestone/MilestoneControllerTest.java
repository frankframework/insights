package org.frankframework.insights.milestone;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import org.frankframework.insights.common.mapper.MappingException;

import org.frankframework.insights.github.GitHubPropertyState;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MilestoneController.class)
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
	public void getAllOpenMilestones_returnsMilestones() throws Exception {
		MilestoneResponse m1 = new MilestoneResponse("id1", 1, "v1.0", GitHubPropertyState.OPEN);
		MilestoneResponse m2 = new MilestoneResponse("id2", 2, "v2.0", GitHubPropertyState.OPEN);
		Set<MilestoneResponse> milestones = Set.of(m1, m2);

		when(milestoneService.getAllOpenMilestones()).thenReturn(milestones);

		mockMvc.perform(get("/api/milestones/open"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	public void getAllOpenMilestones_returnsEmptySet() throws Exception {
		when(milestoneService.getAllOpenMilestones()).thenReturn(Collections.emptySet());

		mockMvc.perform(get("/api/milestones/open"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	public void getAllOpenMilestones_serviceReturnsNull_treatedAsEmptySet() throws Exception {
		when(milestoneService.getAllOpenMilestones()).thenReturn(null);

		mockMvc.perform(get("/api/milestones/open"))
				.andExpect(status().isOk() )
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	public void getAllOpenMilestones_serviceThrowsMappingException_returns400() throws Exception {
		when(milestoneService.getAllOpenMilestones()).thenThrow(new MappingException("fail", null));

		mockMvc.perform(get("/api/milestones/open"))
				.andExpect(status().isBadRequest());
	}
}
