package org.frankframework.insights.release;

import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.commit.Commit;
import org.frankframework.insights.common.entityconnection.ReleaseCommit;
import org.frankframework.insights.common.entityconnection.branchcommit.BranchCommit;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReleaseService {
    private final GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;
    private final GitHubClient gitHubClient;
    private final Mapper mapper;
    private final ReleaseRepository releaseRepository;
    private final BranchService branchService;

    public ReleaseService(
            GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
            GitHubClient gitHubClient,
            Mapper mapper,
            ReleaseRepository releaseRepository,
            BranchService branchService) {
        this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.releaseRepository = releaseRepository;
        this.branchService = branchService;
    }

    public void injectReleases() throws ReleaseInjectionException {
        if (gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubReleaseCount()
                == releaseRepository.count()) {
            log.info("Releases already exist in the database.");
            return;
        }

        try {
            log.info("Fetching GitHub releases...");
            Set<ReleaseDTO> releaseDTOs = gitHubClient.getReleases();
            List<Branch> branches = branchService.getAllBranchesWithCommits();

            List<Release> releasesWithBranches = releaseDTOs.stream()
                    .map(dto -> createReleaseWithBranch(dto, branches))
                    .filter(Objects::nonNull)
                    .toList();

            if (releasesWithBranches.isEmpty()) {
                log.info("No releases with detected base branch to inject.");
                return;
            }

            Set<Release> releasesWithNewCommits = assignNewCommitsToReleases(releasesWithBranches);

            saveAllReleases(releasesWithNewCommits);
        } catch (Exception e) {
            throw new ReleaseInjectionException("Error injecting GitHub releases.", e);
        }
    }

    private Release createReleaseWithBranch(ReleaseDTO dto, List<Branch> branches) {
        if (dto.getTagCommit() == null || dto.getTagCommit().getCommitSha() == null || branches.isEmpty()) {
            log.warn("Skipping release '{}' due to missing info.", dto.getTagName());
            return null;
        }

        Release release = mapper.toEntity(dto, Release.class);
        release.setBranch(findBranchForRelease(release, branches));

        if (release.getBranch() == null) {
            log.warn("No matching branch found for release '{}'.", release.getTagName());
            return null;
        }

        return release;
    }

    private Branch findBranchForRelease(Release release, List<Branch> branches) {
        return branches.stream()
                .filter(branch -> branchService.doesBranchContainCommit(branch, release.getCommitSha()))
                .max(Comparator.comparing(branch -> "master".equals(branch.getName()) ? 1 : 0))
                .orElse(null);
    }

    private Set<Release> assignNewCommitsToReleases(List<Release> releases) {
        Map<Branch, List<Release>> releasesByBranch = releases.stream()
                .filter(release -> release.getBranch() != null)
                .collect(Collectors.groupingBy(Release::getBranch));

        Set<Release> updatedReleases = new HashSet<>();

        for (Map.Entry<Branch, List<Release>> entry : releasesByBranch.entrySet()) {
            Branch branch = entry.getKey();
            List<Release> branchReleases = entry.getValue();

            if (branch == null || branchReleases.isEmpty()) {
                return new HashSet<>(branchReleases);
            }

            branchReleases.sort(Comparator.comparing(Release::getPublishedAt));

            TreeSet<Release> sortedReleases = new TreeSet<>(Comparator.comparing(Release::getPublishedAt));

            for (Release release : branchReleases) {
                Set<Commit> newCommits = new HashSet<>();

                Release previousRelease = sortedReleases.lower(release);

                if (previousRelease == null) {
                    newCommits.addAll(branch.getBranchCommits().stream()
                            .map(BranchCommit::getCommit)
                            .filter(commit -> !commit.getCommittedDate().isAfter(release.getPublishedAt()))
                            .toList());
                } else {
                    newCommits.addAll(branch.getBranchCommits().stream()
                            .map(BranchCommit::getCommit)
                            .filter(commit -> commit.getCommittedDate().isAfter(previousRelease.getPublishedAt())
                                    && !commit.getCommittedDate().isAfter(release.getPublishedAt()))
                            .toList());
                }

                Set<ReleaseCommit> newReleaseCommits = newCommits.stream()
                        .map(commit -> new ReleaseCommit(release, commit))
                        .collect(Collectors.toSet());

                release.setReleaseCommits(newReleaseCommits);

                updatedReleases.add(release);
                sortedReleases.add(release);
            }
        }

        return updatedReleases;
    }

    private void saveAllReleases(Set<Release> releases) {
        List<Release> savedReleases = releaseRepository.saveAll(releases);
        log.info("Successfully saved {} releases.", savedReleases.size());
    }
}
