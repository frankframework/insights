package org.frankframework.insights.commit;

import jakarta.transaction.Transactional;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.mapper.Mapper;
import org.frankframework.insights.models.Commit;
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
            Set<Branch> branchesIncludingCommits = new HashSet<>();

            log.info("Successfully fetched branches and found: {}", branches.size());

            for (Branch branch : branches) {
                Set<CommitDTO> commitDTOS = gitHubClient.getBranchCommits(branch.getName());
                Set<Commit> commits = commitMapper.toEntity(commitDTOS, Commit.class);
                branch.setCommits(commits);
                branchesIncludingCommits.add(branch);
                log.info("Setted commits to branch {}", branch.getName());
            }

            branchService.saveBranches(branchesIncludingCommits);
        } catch (Exception e) {
            throw new CommitInjectionException("Error while injecting GitHub commits and setting them to branches", e);
        }
    }
}
