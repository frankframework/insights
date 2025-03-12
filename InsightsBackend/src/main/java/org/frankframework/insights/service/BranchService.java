package org.frankframework.insights.service;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.configuration.GitHubProperties;
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
    private final String protectionRegex;

    public BranchService(
            GitHubClient gitHubClient,
            Mapper branchMapper,
            BranchRepository branchRepository,
            GitHubProperties gitHubProperties) {
        this.gitHubClient = gitHubClient;
        this.branchMapper = branchMapper;
        this.branchRepository = branchRepository;
        this.protectionRegex = gitHubProperties.getProtectionRegex();
    }

    public void injectBranches() throws BranchInjectionException {
        if (!branchRepository.findAll().isEmpty()) {
            log.info("Branches already found in the in database");
            return;
        }

        try {
            log.info("Start injecting GitHub branches");
            Set<BranchDTO> branchDTOs = gitHubClient.getBranches();
            Set<Branch> branches = filterBranchesByProtectionPattern(branchDTOs);
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

    private Set<Branch> filterBranchesByProtectionPattern(Set<BranchDTO> branchDTOs) {
        log.info("Filtering branches by protection pattern: {}, and maps them to database entities", protectionRegex);
        Set<Branch> filteredBranches = branchDTOs.stream()
                .filter(branchDTO -> Pattern.compile(protectionRegex)
                        .matcher(branchDTO.getName())
                        .find())
                .map(branchDTO -> branchMapper.toEntity(branchDTO, Branch.class))
                .collect(Collectors.toSet());

        log.info(
                "Successfully filtered {} branches by protection pattern: {}, and mapped them.",
                filteredBranches.size(),
                protectionRegex);
        return filteredBranches;
    }

    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    public void saveBranches(Set<Branch> branches) throws BranchDatabaseException {
        try {
            branchRepository.saveAll(branches);
            log.info("Successfully saved {} branches.", branches.size());
        } catch (Exception e) {
            throw new BranchDatabaseException("Error while saving set of branches", e);
        }
    }
}
