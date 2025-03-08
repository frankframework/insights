package org.frankframework.insights.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
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

    @Transactional
    public void injectBranchCommits() throws RuntimeException {
        if (!commitRepository.findAll().isEmpty()) {
            return;
        }

        try {
            List<Branch> branches = branchService.getAllBranches();

            for (Branch branch : branches) {
                Set<CommitDTO> commitDTOS = gitHubClient.getBranchCommits(branch.getName());
                Set<Commit> commits = commitMapper.toEntity(commitDTOS);

                saveCommits(commits);

                branch.setCommits(commits);
                branchService.saveOrUpdateBranch(branch);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching commits", e);
        }
    }

    private void saveCommits(Set<Commit> commits) {
        Set<String> existingCommitShas = commitRepository.findAllOid();

        Set<Commit> newCommits = commits.stream()
                .filter(commit -> !existingCommitShas.contains(commit.getOid()))
                .collect(Collectors.toSet());

        commitRepository.saveAll(newCommits);
    }
}
