package org.frankframework.insights.release;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Set;

import org.frankframework.insights.branch.BranchResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReleaseController.class)
public class ReleaseControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ReleaseService releaseService;

	@TestConfiguration
	public static class TestConfig {
		@Bean
		public ReleaseService releaseService() {
			return mock(ReleaseService.class);
		}
	}

	@BeforeEach
	public void resetMocks() {
		reset(releaseService);
	}

	@Test
	public void getAllReleases_returnsOkWithReleases() throws Exception {
		BranchResponse branchResponse = Mockito.mock(BranchResponse.class);
		ReleaseResponse response1 = new ReleaseResponse("id1", "tag1", "name1", OffsetDateTime.now(), "sha1", branchResponse);
		ReleaseResponse response2 = new ReleaseResponse("id2", "tag2", "name2", OffsetDateTime.now().minusDays(1), "sha2", branchResponse);
		Set<ReleaseResponse> releases = Set.of(response1, response2);

		when(releaseService.getAllReleases()).thenReturn(releases);

		mockMvc.perform(get("/api/releases"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	public void getAllReleases_returnsEmptySet() throws Exception {
		when(releaseService.getAllReleases()).thenReturn(Collections.emptySet());

		mockMvc.perform(get("/api/releases"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	public void getAllReleases_serviceReturnsNull_treatedAsEmptySet() throws Exception {
		when(releaseService.getAllReleases()).thenReturn(null);

		mockMvc.perform(get("/api/releases"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(0));
	}
}
