package org.frankframework.insights.service;

import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.dto.BranchDTO;
import org.frankframework.insights.exceptions.branches.BranchDatabaseException;
import org.frankframework.insights.exceptions.branches.BranchInjectionException;
import org.frankframework.insights.mapper.Mapper;
import org.frankframework.insights.models.Branch;
import org.frankframework.insights.repository.BranchRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BranchService {
    private final GitHubClient gitHubClient;
    private final Mapper branchMapper;
    private final BranchRepository branchRepository;

    public BranchService(GitHubClient gitHubClient, Mapper branchMapper, BranchRepository branchRepository) {
        this.gitHubClient = gitHubClient;
        this.branchMapper = branchMapper;
        this.branchRepository = branchRepository;
    }

    public void injectBranches() throws BranchInjectionException {
        if (!branchRepository.findAll().isEmpty()) {
            log.info("Branches already found in the in database");
            return;
        }

        try {
            log.info("Start injecting GitHub branches");
            Set<BranchDTO> branchDTOs = gitHubClient.getBranches();
            Set<Branch> branches = branchMapper.toEntity(branchDTOs, Branch.class);
            saveBranches(branches);
        } catch (Exception e) {
            throw new BranchInjectionException("Error while injecting GitHub branches", e);
        }
    }

    public boolean doesBranchContainCommit(Branch branch, String commitOid) throws BranchDatabaseException {
        try {
            boolean containsCommit = branch.getCommits().stream()
                    .anyMatch(commit -> commit.getOid().equals(commitOid));

            log.info("Branch {} contains commit: {}", branch.getName(), containsCommit);

            return containsCommit;

        } catch (Exception e) {
            log.error("Error while checking if branch contains commit: {}", commitOid, e);
            throw new BranchDatabaseException("Error while checking if branch contains the commit hash", e);
        }
    }

    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    public void saveOrUpdateBranch(Branch branch) throws BranchDatabaseException {
        try {
            branchRepository.save(branch);
            log.info("Successfully updated and saved branches");
        } catch (Exception e) {
            throw new BranchDatabaseException("Error while updating and saving branches", e);
        }
    }

    private void saveBranches(Set<Branch> branches) throws BranchDatabaseException {
        try {
            branchRepository.saveAll(branches);
        } catch (Exception e) {
            throw new BranchDatabaseException("Error while saving set of branches", e);
        }
    }
}
