package org.frankframework.insights.release;

import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.commit.Commit;
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
            List<Branch> branches = branchService.getAllBranches();

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
        if (dto.getTagCommit() == null || dto.getTagCommit().getOid() == null) {
            log.warn("Skipping release '{}' due to missing commit info.", dto.getTagName());
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
                .filter(branch -> branchService.doesBranchContainCommit(branch, release.getOid()))
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

            branchReleases.sort(Comparator.comparing(Release::getPublishedAt));

            TreeSet<Release> sortedReleases = new TreeSet<>(Comparator.comparing(Release::getPublishedAt));

            for (Release release : branchReleases) {
                Set<Commit> newCommits = new HashSet<>();

                Release previousRelease = sortedReleases.lower(release);

                if (previousRelease == null) {
                    newCommits.addAll(branch.getCommits().stream()
                            .filter(commit -> !commit.getCommittedDate().isAfter(release.getPublishedAt()))
                            .toList());
                } else {
                    newCommits.addAll(branch.getCommits().stream()
                            .filter(commit -> commit.getCommittedDate().isAfter(previousRelease.getPublishedAt()) &&
                                    !commit.getCommittedDate().isAfter(release.getPublishedAt()))
                            .toList());
                }

                release.setReleaseCommits(newCommits);
                updatedReleases.add(release);
                sortedReleases.add(release);
            }
        }

        return updatedReleases;
    }

    private void saveAllReleases(Set<Release> releases) {
        releaseRepository.saveAll(releases);
        log.info("Successfully saved {} releases.", releases.size());
    }
}
