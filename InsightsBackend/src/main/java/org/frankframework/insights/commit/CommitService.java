package org.frankframework.insights.commit;

import java.util.*;
import java.util.stream.Collectors;
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

    public void injectBranchCommits() {
        Map<String, Integer> githubCommitCounts = fetchGitHubCommitCounts();

        List<Branch> branches = branchService.getAllBranches();
        log.info("Fetched {} branches", branches.size());

        List<Branch> branchesToUpdate = filterBranchesToUpdate(branches, githubCommitCounts);
        log.info("Found {} branches to update", branchesToUpdate.size());

        if (branchesToUpdate.isEmpty()) {
            log.info("No branches to update commits of. Skipping...");
            return;
        }

        updateBranches(branchesToUpdate);
    }

    private Map<String, Integer> fetchGitHubCommitCounts() {
        return gitHubRepositoryStatisticsService
                .getGitHubRepositoryStatisticsDTO()
                .getGitHubCommitsCount(branchProtectionRegexes);
    }

    public List<Branch> filterBranchesToUpdate(List<Branch> branches, Map<String, Integer> githubCommitCounts) {
        return branches.stream()
                .filter(branch -> {
                    int databaseCommitCount = branchCommitRepository.countBranchCommitByBranch(branch);
                    int githubCommitCount = githubCommitCounts.getOrDefault(branch.getName(), 0);

                    log.info(
                            "{} commits found in database, {} commits found in GitHub, for branch {}",
                            databaseCommitCount,
                            githubCommitCount,
                            branch.getName());

                    return databaseCommitCount != githubCommitCount;
                })
                .toList();
    }

    private void updateBranches(List<Branch> branchesToUpdate) {
        Set<Branch> updatedBranches = branchesToUpdate.stream()
                .map(branch -> {
                    try {
                        return getCommitsForBranch(branch);
                    } catch (CommitInjectionException e) {
                        log.error("Failed to update commits for branch: {}", branch.getName(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        branchService.saveBranches(updatedBranches);
    }

    private Branch getCommitsForBranch(Branch branch) throws CommitInjectionException {
        try {
            Set<CommitDTO> commitDTOs = gitHubClient.getBranchCommits(branch.getName());
            Set<Commit> commits = mapper.toEntity(commitDTOs, Commit.class);

            Set<BranchCommit> branchCommits = new HashSet<>(getBranchCommitsForBranch(branch));

            Set<BranchCommit> updatedBranchCommits = processBranchCommits(branch, commits, branchCommits);

            branchCommits.clear();
            branchCommits.addAll(updatedBranchCommits);
            branch.setBranchCommits(branchCommits);

            log.info("Updated branch {} with {} commits", branch.getName(), updatedBranchCommits.size());
            return branch;
        } catch (Exception e) {
            throw new CommitInjectionException("Error while injecting GitHub commits and setting them to branches", e);
        }
    }

    private Set<BranchCommit> getBranchCommitsForBranch(Branch branch) {
        return branchCommitRepository.findBranchCommitByBranchId(branch.getId());
    }

    private Set<BranchCommit> processBranchCommits(
            Branch branch, Set<Commit> commits, Set<BranchCommit> existingBranchCommits) {
        Map<String, BranchCommit> existingCommitsMap = existingBranchCommits.stream()
                .collect(Collectors.toMap(bc -> buildUniqueKey(bc.getBranch(), bc.getCommit()), bc -> bc));

        return commits.stream()
                .map(commit -> getOrCreateBranchCommit(branch, commit, existingCommitsMap))
                .collect(Collectors.toSet());
    }

    private BranchCommit getOrCreateBranchCommit(
            Branch branch, Commit commit, Map<String, BranchCommit> existingCommitsMap) {
        String key = buildUniqueKey(branch, commit);
        return existingCommitsMap.getOrDefault(key, new BranchCommit(branch, commit));
    }

    private String buildUniqueKey(Branch branch, Commit commit) {
        return String.format("%s::%s", branch.getId(), commit.getId());
    }
}
