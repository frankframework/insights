package org.frankframework.insights.service;

import java.util.*;
import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.dto.MatchingBranch;
import org.frankframework.insights.dto.ReleaseDTO;
import org.frankframework.insights.mapper.ReleaseMapper;
import org.frankframework.insights.models.Branch;
import org.frankframework.insights.models.Commit;
import org.frankframework.insights.models.Release;
import org.frankframework.insights.repository.ReleaseRepository;
import org.springframework.stereotype.Service;

@Service
public class ReleaseService {
    private final GitHubClient gitHubClient;

    private final ReleaseMapper releaseMapper;

    private final ReleaseRepository releaseRepository;
    private final BranchService branchService;

    public ReleaseService(
            GitHubClient gitHubClient,
            ReleaseMapper releaseMapper,
            ReleaseRepository releaseRepository,
            BranchService branchService) {
        this.gitHubClient = gitHubClient;
        this.releaseMapper = releaseMapper;
        this.releaseRepository = releaseRepository;
        this.branchService = branchService;
    }

    public void injectReleases() {
        if (!releaseRepository.findAll().isEmpty()) {
            return;
        }

        try {
            Set<ReleaseDTO> releaseDTOs = gitHubClient.getReleases();
            Set<Release> releases = releaseMapper.toEntity(releaseDTOs);

            Map<String, MatchingBranch> releaseBranches = stripReleaseBranches(releaseDTOs);

            for (Release release : releases) {
                release.setBranch(releaseBranches.get(release.getTagName()).branch());
                release.setCommits(releaseBranches.get(release.getTagName()).commits());
                saveRelease(release);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, MatchingBranch> stripReleaseBranches(Set<ReleaseDTO> releaseDTOs) {
        List<Branch> branches = branchService.getAllBranches();
        Map<String, MatchingBranch> releaseBranches = new HashMap<>();

        for (ReleaseDTO releaseDTO : releaseDTOs) {
            Set<Branch> matchingBranches =
                    findMatchingBranches(branches, releaseDTO.getTagCommit().getOid());

            MatchingBranch calculatedBranch = weighBestMatchingBranch(
                    matchingBranches, releaseDTO.getTagCommit().getOid());

            releaseBranches.put(releaseDTO.getTagName(), calculatedBranch);
        }

        return releaseBranches;
    }

    private Set<Branch> findMatchingBranches(List<Branch> branches, String commitOid) {
        Set<Branch> matchingBranches = new HashSet<>();

        for (Branch branch : branches) {
            if (branchService.doesBranchContainCommit(branch, commitOid)) {
                matchingBranches.add(branch);
            }
        }

        return matchingBranches;
    }

    private MatchingBranch weighBestMatchingBranch(Set<Branch> branches, String releaseCommitOid) {
        Map<Branch, Float> matchingBranches = new HashMap<>();
        Map<Branch, Set<Commit>> branchReleaseCommits = new HashMap<>();

        for (Branch branch : branches) {
            List<Commit> sortedCommits = branch.getCommits().stream()
                    .sorted(Comparator.comparing(Commit::getTimestamp))
                    .toList();

            List<Commit> commitsBeforeRelease = sortedCommits.stream()
                    .takeWhile(commit -> !commit.getSha().equals(releaseCommitOid))
                    .toList();

            branchReleaseCommits.put(branch, new HashSet<>(commitsBeforeRelease));

            if ("master".equals(branch.getName())) {
                return new MatchingBranch(branch, new HashSet<>(commitsBeforeRelease));
            }

            float ratio = calculateBestMatchingBranch(sortedCommits.size(), commitsBeforeRelease.size());
            matchingBranches.put(branch, ratio);
        }

        return matchingBranches.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> new MatchingBranch(entry.getKey(), branchReleaseCommits.get(entry.getKey())))
                .orElse(null);
    }

    private float calculateBestMatchingBranch(int totalAmountCommits, int amountOfCommitsBeforeReleaseCommit) {
        return (float) totalAmountCommits / amountOfCommitsBeforeReleaseCommit;
    }

    private void saveRelease(Release release) {
        releaseRepository.save(release);
    }
}
