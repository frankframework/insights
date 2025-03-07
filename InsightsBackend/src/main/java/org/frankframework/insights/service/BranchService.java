package org.frankframework.insights.service;

import java.util.List;
import java.util.Set;
import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.dto.BranchDTO;
import org.frankframework.insights.mapper.BranchMapper;
import org.frankframework.insights.models.Branch;
import org.frankframework.insights.repository.BranchRepository;
import org.springframework.stereotype.Service;

@Service
public class BranchService {
    private final GitHubClient gitHubClient;
    private final BranchMapper branchMapper;
    private final BranchRepository branchRepository;

    public BranchService(GitHubClient gitHubClient, BranchMapper branchMapper, BranchRepository branchRepository) {
        this.gitHubClient = gitHubClient;
        this.branchMapper = branchMapper;
        this.branchRepository = branchRepository;
    }

    public void injectBranches() throws RuntimeException {
        if (!branchRepository.findAll().isEmpty()) {
            return;
        }

        try {
            Set<BranchDTO> branchDTOs = gitHubClient.getBranches();
            Set<Branch> branches = branchMapper.toEntity(branchDTOs);
            saveBranches(branches);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching commits", e);
        }
    }

    public boolean doesBranchContainCommit(Branch branch, String commitOid) {
        return branch.getCommits().stream().anyMatch(commit -> commit.getSha().equals(commitOid));
    }

    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    public void saveOrUpdateBranch(Branch branch) {
        branchRepository.save(branch);
    }

    private void saveBranches(Set<Branch> branches) {
        branchRepository.saveAll(branches);
    }
}
