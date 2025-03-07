package org.frankframework.insights.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.dto.CommitDTO;
import org.frankframework.insights.mapper.CommitMapper;
import org.frankframework.insights.models.Branch;
import org.frankframework.insights.models.Commit;
import org.frankframework.insights.repository.CommitRepository;
import org.springframework.stereotype.Service;

@Service
public class CommitService {
    private final GitHubClient gitHubClient;
    private final CommitMapper commitMapper;
    private final CommitRepository commitRepository;
    private final BranchService branchService;

    public CommitService(
            GitHubClient gitHubClient,
            CommitMapper commitMapper,
            CommitRepository commitRepository,
            BranchService branchService) {
        this.gitHubClient = gitHubClient;
        this.commitMapper = commitMapper;
        this.commitRepository = commitRepository;
        this.branchService = branchService;
    }

    public void injectBranchCommits() throws RuntimeException {
        if (!commitRepository.findAll().isEmpty()) {
            return;
        }

        try {
            List<Branch> branches = branchService.getAllBranches();

            for (Branch branch : branches) {
                Set<CommitDTO> commitDTOS = gitHubClient.getBranchCommits(branch.getName());
                Set<Commit> commits = commitMapper.toEntity(commitDTOS);
                branch.setCommits(commits);

                saveCommits(commits);

                branchService.saveOrUpdateBranch(branch);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching commits", e);
        }
    }

    public void saveCommits(Set<Commit> commits) {
        Set<String> existingCommitIds =
                commitRepository.findAll().stream().map(Commit::getId).collect(Collectors.toSet());

        Set<Commit> newCommits = commits.stream()
                .filter(commit -> !existingCommitIds.contains(commit.getId()))
                .collect(Collectors.toSet());

        commitRepository.saveAll(newCommits);
    }
}
