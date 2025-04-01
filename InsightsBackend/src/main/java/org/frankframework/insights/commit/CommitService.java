package org.frankframework.insights.commit;

import java.util.*;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.common.configuration.GitHubProperties;
import org.frankframework.insights.common.entityconnection.branchcommit.BranchCommit;
import org.frankframework.insights.common.entityconnection.branchcommit.BranchCommitRepository;
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
    private final BranchCommitRepository branchCommitRepository;
    private final BranchService branchService;
    private final List<String> branchProtectionRegexes;

    public CommitService(
            GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
            GitHubClient gitHubClient,
            Mapper mapper,
            BranchCommitRepository branchCommitRepository,
            BranchService branchService,
            GitHubProperties gitHubProperties) {
        this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.branchCommitRepository = branchCommitRepository;
        this.branchService = branchService;
        this.branchProtectionRegexes = gitHubProperties.getBranchProtectionRegexes();
    }

    public void injectBranchCommits() throws CommitInjectionException {
        if (gitHubRepositoryStatisticsService
                        .getGitHubRepositoryStatisticsDTO()
                        .getGitHubCommitCount(branchProtectionRegexes)
                == branchCommitRepository.count()) {
            log.info("Branch commits already found in the in database");
            return;
        }

        try {
            log.info("Start injecting GitHub commits for each branch");
            List<Branch> branches = branchService.getAllBranches();
            log.info("Successfully fetched branches and found: {}", branches.size());

            Set<Branch> branchesWithCommits =
                    branches.stream().map(this::getCommitsForBranch).collect(Collectors.toSet());

            branchService.saveBranches(branchesWithCommits);
        } catch (Exception e) {
            throw new CommitInjectionException("Error while injecting GitHub commits and setting them to branches", e);
        }
    }

    @SneakyThrows
    private Branch getCommitsForBranch(Branch branch) {
        Set<CommitDTO> commitDTOs = gitHubClient.getBranchCommits(branch.getName());
        Set<Commit> commits = mapper.toEntity(commitDTOs, Commit.class);

        branch.setBranchCommits(
                commits.stream().map(commit -> new BranchCommit(branch, commit)).collect(Collectors.toSet()));

        log.info("Setted commits to branch {}", branch.getName());
        return branch;
    }
}
