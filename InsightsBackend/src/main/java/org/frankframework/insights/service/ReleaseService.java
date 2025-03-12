package org.frankframework.insights.service;

import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.dto.ReleaseDTO;
import org.frankframework.insights.exceptions.branches.BranchDatabaseException;
import org.frankframework.insights.exceptions.releases.ReleaseDatabaseException;
import org.frankframework.insights.exceptions.releases.ReleaseInjectionException;
import org.frankframework.insights.mapper.Mapper;
import org.frankframework.insights.models.Branch;
import org.frankframework.insights.models.Commit;
import org.frankframework.insights.models.Release;
import org.frankframework.insights.repository.ReleaseRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReleaseService {
    private final GitHubClient gitHubClient;
    private final Mapper releaseMapper;
    private final ReleaseRepository releaseRepository;
    private final BranchService branchService;

    public ReleaseService(
            GitHubClient gitHubClient,
            Mapper releaseMapper,
            ReleaseRepository releaseRepository,
            BranchService branchService) {
        this.gitHubClient = gitHubClient;
        this.releaseMapper = releaseMapper;
        this.releaseRepository = releaseRepository;
        this.branchService = branchService;
    }

    public void injectReleases() throws ReleaseInjectionException {
        if (!releaseRepository.findAll().isEmpty()) {
            log.info("Releases already exist in the database.");
            return;
        }

        try {
            log.info("Fetching GitHub releases...");
            Set<ReleaseDTO> releaseDTOs = gitHubClient.getReleases();
            List<Branch> branches = branchService.getAllBranches();

            Set<Release> releases = releaseDTOs.stream()
                    .map(dto -> createReleaseWithBranchAndCommits(dto, branches))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // If no releases remain after filtering, do not call saveAllReleases.
            if (releases.isEmpty()) {
                log.info("No releases to inject after filtering.");
                return;
            }

            saveAllReleases(releases);
        } catch (Exception e) {
            throw new ReleaseInjectionException("Error injecting GitHub releases.", e);
        }
    }

    private Release createReleaseWithBranchAndCommits(ReleaseDTO dto, List<Branch> branches) {
        if (dto.getTagCommit() == null || dto.getTagCommit().getOid() == null) {
            log.warn("Skipping release '{}' due to missing commit info.", dto.getTagName());
            return null;
        }

        Release release = releaseMapper.toEntity(dto, Release.class);
        release = setBranchForRelease(release, branches);

        // If no matching branch found, skip this release.
        if (release.getBranch() == null) {
            log.warn("No matching branch found for release '{}'.", release.getTagName());
            return null;
        }

        release = setNewCommitsForRelease(release);
        return release;
    }

    public Release setBranchForRelease(Release release, List<Branch> branches) {
        Optional<Branch> bestBranch = branches.stream()
                .filter(branch -> doesBranchContainCommit(branch, release.getOid()))
                .max(Comparator.comparing(branch -> "master".equals(branch.getName()) ? 1 : 0));

        bestBranch.ifPresentOrElse(
                release::setBranch, () -> log.warn("No matching branch found for release '{}'.", release.getTagName()));

        return release;
    }

    private boolean doesBranchContainCommit(Branch branch, String commitOid) {
        try {
            return branchService.doesBranchContainCommit(branch, commitOid);
        } catch (BranchDatabaseException e) {
            throw new RuntimeException(e);
        }
    }

    public Release setNewCommitsForRelease(Release release) {
        Branch branch = release.getBranch();
        if (branch == null) return release;

        List<Commit> branchCommits = branch.getCommits().stream()
                .sorted(Comparator.comparing(Commit::getCommittedDate))
                .toList();

        Set<Commit> newCommits = new HashSet<>(branchCommits);
        release.setReleaseCommits(newCommits);

        return release;
    }

    private void saveAllReleases(Set<Release> releases) throws ReleaseDatabaseException {
        try {
            releaseRepository.saveAll(releases);
            log.info("Successfully saved {} releases.", releases.size());
        } catch (Exception e) {
            log.error("Error saving releases: {}", e.getMessage(), e);
            throw new ReleaseDatabaseException("Error occurred while saving releases.", e);
        }
    }
}
