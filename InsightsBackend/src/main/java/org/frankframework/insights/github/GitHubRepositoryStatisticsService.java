package org.frankframework.insights.github;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.configuration.GitHubProperties;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GitHubRepositoryStatisticsService {

	private final GitHubClient gitHubClient;
	private final List<String> branchProtectionRegexes;

	@Getter
	private GitHubRepositoryStatisticsDTO gitHubRepositoryStatisticsDTO;

	public GitHubRepositoryStatisticsService(GitHubClient gitHubClient, GitHubProperties gitHubProperties) {
		this.gitHubClient = gitHubClient;
		this.branchProtectionRegexes = gitHubProperties.getBranchProtectionRegexes();
	}

	public void fetchRepositoryStatistics() throws GitHubClientException {
		GitHubRepositoryStatisticsDTO statisticsDTO = gitHubClient.getRepositoryStatistics();

		List<GitHubRefsDTO.GitHubBranchNodeDTO> protectedBranches = statisticsDTO.getGitHubBranches().nodes().stream()
				.filter(branch -> branchProtectionRegexes.stream()
						.anyMatch(regex -> Pattern.compile(regex).matcher(branch.name()).find()))
				.collect(Collectors.toList());

		for (GitHubRefsDTO.GitHubBranchNodeDTO branch : protectedBranches) {
			try {
				GitHubRefsDTO.GitHubBranchStatisticsDTO branchStats = gitHubClient.getBranchStatistics(String.format("refs/heads/%s", branch.name()), branch.name());

				GitHubRefsDTO.GitHubBranchNodeDTO updatedBranch = new GitHubRefsDTO.GitHubBranchNodeDTO(
						branch.name(),
						branchStats.ref().target(),
						branchStats.pullRequests()
				);

				protectedBranches = protectedBranches.stream()
						.map(existingBranch -> existingBranch.name().equals(updatedBranch.name()) ? updatedBranch : existingBranch)
						.toList();

			} catch (Exception e) {
				log.warn("Failed to fetch stats for protected branch '{}': {}", branch.name(), e.getMessage());
			}
		}

		gitHubRepositoryStatisticsDTO = new GitHubRepositoryStatisticsDTO(
				statisticsDTO.labels(),
				statisticsDTO.milestones(),
				new GitHubRefsDTO(protectedBranches),
				statisticsDTO.releases(),
				statisticsDTO.issues()
		);
	}
}
