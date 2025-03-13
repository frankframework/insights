package org.frankframework.insights.commit;

import jakarta.transaction.Transactional;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.common.configuration.GitHubProperties;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;

import org.frankframework.insights.github.GitHubRepositoryStatisticsService;

import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CommitService {
	private final GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;
    private final GitHubClient gitHubClient;
    private final Mapper mapper;
    private final CommitRepository commitRepository;
    private final BranchService branchService;
	private final List<String> branchProtectionRegexes;

    public CommitService(
			GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
			GitHubClient gitHubClient,
			Mapper mapper,
			CommitRepository commitRepository,
			BranchService branchService,
			GitHubProperties gitHubProperties) {
		this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.commitRepository = commitRepository;
        this.branchService = branchService;
		this.branchProtectionRegexes = gitHubProperties.getBranchProtectionRegexes();
    }

    public void injectBranchCommits() throws CommitInjectionException {
        if (gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubCommitCount(branchProtectionRegexes) == commitRepository.countAllBranchCommitRelations()) {
            log.info("Branch commits already found in the in database");
            return;
        }

        try {
            log.info("Start injecting GitHub commits for each branch");
            List<Branch> branches = branchService.getAllBranches();
            Set<Branch> branchesWithCommits = new HashSet<>();

            log.info("Successfully fetched branches and found: {}", branches.size());

            for (Branch branch : branches) {
                Set<CommitDTO> commitDTOS = gitHubClient.getBranchCommits(branch.getName());
                Set<Commit> commits = mapper.toEntity(commitDTOS, Commit.class);
                branch.setCommits(commits);
				branchesWithCommits.add(branch);
                log.info("Setted commits to branch {}", branch.getName());
            }

            branchService.saveBranches(branchesWithCommits);
        } catch (Exception e) {
            throw new CommitInjectionException("Error while injecting GitHub commits and setting them to branches", e);
        }
    }
}
