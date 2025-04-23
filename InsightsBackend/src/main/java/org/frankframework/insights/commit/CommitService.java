package org.frankframework.insights.commit;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.common.configuration.properties.GitHubProperties;
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
    private final CommitRepository commitRepository;

    public CommitService(
            GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
            GitHubClient gitHubClient,
            Mapper mapper,
            BranchCommitRepository branchCommitRepository,
            BranchService branchService,
            GitHubProperties gitHubProperties,
            CommitRepository commitRepository) {
        this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.branchCommitRepository = branchCommitRepository;
        this.branchService = branchService;
        this.branchProtectionRegexes = gitHubProperties.getBranchProtectionRegexes();
        this.commitRepository = commitRepository;
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

        injectNewBranchCommits(branchesToUpdate);
    }

    private Map<String, Integer> fetchGitHubCommitCounts() {
        return gitHubRepositoryStatisticsService
                .getGitHubRepositoryStatisticsDTO()
                .getGitHubCommitsCount(branchProtectionRegexes);
    }

    private List<Branch> filterBranchesToUpdate(List<Branch> branches, Map<String, Integer> githubCommitCounts) {
        Set<String> branchesToUpdate = branches.stream()
                .filter(branch -> shouldUpdateBranch(branch, githubCommitCounts))
                .map(Branch::getName)
                .collect(Collectors.toSet());

        return branches.stream()
                .filter(branch -> branchesToUpdate.contains(branch.getName()))
                .collect(Collectors.toList());
    }

    private boolean shouldUpdateBranch(Branch branch, Map<String, Integer> githubCommitCounts) {
        int dbCount = branchCommitRepository.countBranchCommitByBranch_Name(branch.getName());
        int githubCount = githubCommitCounts.getOrDefault(branch.getName(), 0);

        log.info("{} commits in DB, {} in GitHub for branch {}", dbCount, githubCount, branch.getName());
        return dbCount != githubCount;
    }

    private void injectNewBranchCommits(List<Branch> branchesToUpdate) {
        List<BranchCommit> newBranchCommits = branchesToUpdate.stream()
                .flatMap(branch -> {
                    try {
                        Set<BranchCommit> newCommits = getNewBranchCommits(branch);
                        log.info("Prepared {} new commits for branch {}", newCommits.size(), branch.getName());
                        return newCommits.stream();
                    } catch (CommitInjectionException e) {
                        log.error("Failed to fetch commits for branch: {}", branch.getName(), e);
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toList());

        if (!newBranchCommits.isEmpty()) {
            Set<Commit> uniqueCommits =
                    newBranchCommits.stream().map(BranchCommit::getCommit).collect(Collectors.toSet());

            List<String> commitIds = uniqueCommits.stream().map(Commit::getId).toList();

            Map<String, Commit> existingCommitsMap = commitIds.stream()
                    .flatMap(id -> commitRepository.findById(id).stream())
                    .collect(Collectors.toMap(Commit::getId, Function.identity()));

            newBranchCommits.forEach(branchCommit -> {
                String commitId = branchCommit.getCommit().getId();
                if (existingCommitsMap.containsKey(commitId)) {
                    branchCommit.setCommit(existingCommitsMap.get(commitId));
                }
            });

            commitRepository.saveAll(uniqueCommits.stream()
                    .filter(commit -> !existingCommitsMap.containsKey(commit.getId()))
                    .toList());
            branchCommitRepository.saveAll(newBranchCommits);
        }
    }

    private Set<BranchCommit> getNewBranchCommits(Branch branch) throws CommitInjectionException {
        try {
            Set<CommitDTO> commitDTOs = gitHubClient.getBranchCommits(branch.getName());
            Set<Commit> commits = mapper.toEntity(commitDTOs, Commit.class);

            Set<BranchCommit> existing = branchCommitRepository.findAllByBranch_Id(branch.getId());
            Map<String, BranchCommit> existingMap = existing.stream()
                    .collect(Collectors.toMap(bc -> buildUniqueKey(bc.getBranch(), bc.getCommit()), bc -> bc));

            return commits.stream()
                    .filter(commit -> !existingMap.containsKey(buildUniqueKey(branch, commit)))
                    .map(commit -> new BranchCommit(branch, commit))
                    .collect(Collectors.toSet());

        } catch (Exception e) {
            throw new CommitInjectionException(
                    "Error while fetching or mapping commits for branch: " + branch.getName(), e);
        }
    }

    private String buildUniqueKey(Branch branch, Commit commit) {
        return branch.getId() + "::" + commit.getId();
    }
}
