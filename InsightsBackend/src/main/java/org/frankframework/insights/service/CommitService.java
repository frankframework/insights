package org.frankframework.insights.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.dto.CommitDTO;
import org.frankframework.insights.exceptions.branches.BranchInjectionException;
import org.frankframework.insights.exceptions.commits.CommitDatabaseException;
import org.frankframework.insights.exceptions.commits.CommitInjectionException;
import org.frankframework.insights.mapper.Mapper;
import org.frankframework.insights.models.Branch;
import org.frankframework.insights.models.Commit;
import org.frankframework.insights.repository.CommitRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CommitService {
    private final GitHubClient gitHubClient;
    private final Mapper commitMapper;
    private final CommitRepository commitRepository;
    private final BranchService branchService;

    public CommitService(
            GitHubClient gitHubClient,
            Mapper commitMapper,
            CommitRepository commitRepository,
            BranchService branchService) {
        this.gitHubClient = gitHubClient;
        this.commitMapper = commitMapper;
        this.commitRepository = commitRepository;
        this.branchService = branchService;
    }

    @Transactional
    public void injectBranchCommits() throws CommitInjectionException {
        if (!commitRepository.findAll().isEmpty()) {
            log.info("Branch commits already found in the in database");
            return;
        }

        try {
            log.info("Start injecting GitHub commits for each branch");
            List<Branch> branches = branchService.getAllBranches();

            log.info("Successfully fetched branches and found: {}", branches.size());

            for (Branch branch : branches) {
                Set<CommitDTO> commitDTOS = gitHubClient.getBranchCommits(branch.getName());
                Set<Commit> commits = commitMapper.toEntity(commitDTOS, Commit.class);

                saveCommits(commits);

                branch.setCommits(commits);
                log.info("Setted commits to branch {}", branch.getName());
                branchService.saveOrUpdateBranch(branch);
            }
        } catch (Exception e) {
            throw new CommitInjectionException("Error while injecting GitHub commits and setting them to branches", e);
        }
    }

    private void saveCommits(Set<Commit> commits) throws CommitDatabaseException {
        try {
            Set<String> existingCommitShas = commitRepository.findAllOid();
            log.info("Found {} existing commits", existingCommitShas.size());
            Set<Commit> newCommits = commits.stream()
                    .filter(commit -> !existingCommitShas.contains(commit.getOid()))
                    .collect(Collectors.toSet());
            log.info("Found {} new commits", newCommits.size());
            commitRepository.saveAll(newCommits);
            log.info("Successfully saved new commits");
        } catch (Exception e) {
            throw new CommitDatabaseException("Error while saving new commits", e);
        }
    }
}
