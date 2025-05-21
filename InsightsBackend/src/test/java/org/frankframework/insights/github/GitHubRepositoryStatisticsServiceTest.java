package org.frankframework.insights.github;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class GitHubRepositoryStatisticsServiceTest {

	private GitHubClient gitHubClient;
	private GitHubRepositoryStatisticsService service;

	@BeforeEach
	public void setup() {
		gitHubClient = mock(GitHubClient.class);
		service = new GitHubRepositoryStatisticsService(gitHubClient);
	}

	@Test
	public void fetchRepositoryStatistics_success_setsDto() throws Exception {
		GitHubTotalCountDTO totalCountDTO = new GitHubTotalCountDTO(1);

		List<GitHubRefsDTO.GitHubBranchNodeDTO> nodes = List.of(
				new GitHubRefsDTO.GitHubBranchNodeDTO("branch1",
						new GitHubRefsDTO.GitHubTargetDTO(
								new GitHubTotalCountDTO(1)
						)
				)
		);
		GitHubRefsDTO refsDTO = new GitHubRefsDTO(nodes);

		GitHubRepositoryStatisticsDTO dto =
				new GitHubRepositoryStatisticsDTO(totalCountDTO, totalCountDTO, refsDTO, totalCountDTO, totalCountDTO);

		when(gitHubClient.getRepositoryStatistics()).thenReturn(dto);

		service.fetchRepositoryStatistics();

		assertSame(dto, service.getGitHubRepositoryStatisticsDTO());
		verify(gitHubClient, times(1)).getRepositoryStatistics();
	}

	@Test
	public void fetchRepositoryStatistics_whenGitHubClientThrows_setsDtoNullAndThrows() throws Exception {
		when(gitHubClient.getRepositoryStatistics()).thenThrow(new GitHubClientException("fail", null));

		assertThrows(GitHubClientException.class, service::fetchRepositoryStatistics);
		assertNull(service.getGitHubRepositoryStatisticsDTO());
		verify(gitHubClient).getRepositoryStatistics();
	}

	@Test
	public void getGitHubRepositoryStatisticsDTO_initiallyNull() {
		assertNull(service.getGitHubRepositoryStatisticsDTO());
	}
}
