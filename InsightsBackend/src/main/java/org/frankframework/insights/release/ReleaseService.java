package org.frankframework.insights.release;

import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.commit.Commit;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;
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

        if (release.getBranch() == null) {
            log.warn("No matching branch found for release '{}'.", release.getTagName());
            return null;
        }

        release = setNewCommitsForRelease(release);
        return release;
    }

    public Release setBranchForRelease(Release release, List<Branch> branches) {
        Optional<Branch> bestBranch = branches.stream()
                .filter(branch -> branchService.doesBranchContainCommit(branch, release.getOid()))
                .max(Comparator.comparing(branch -> "master".equals(branch.getName()) ? 1 : 0));

        bestBranch.ifPresentOrElse(
                release::setBranch, () -> log.warn("No matching branch found for release '{}'.", release.getTagName()));

        return release;
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

    private void saveAllReleases(Set<Release> releases) {
        releaseRepository.saveAll(releases);
        log.info("Successfully saved {} releases.", releases.size());
    }
}
