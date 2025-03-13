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
        if (gitHubRepositoryStatisticsService.getGitHubRepositoryStatisticsDTO().getGitHubReleaseCount() == releaseRepository.count()) {
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

        Release release = mapper.toEntity(dto, Release.class);

        Release releaseWithBranch = setBranchForRelease(release, branches);

        if (release.getBranch() == null) {
            log.warn("No matching branch found for release '{}'.", releaseWithBranch.getTagName());
            return null;
        }

        return setNewCommitsForRelease(releaseWithBranch);
    }

    private Release setBranchForRelease(Release release, List<Branch> branches) {
        Optional<Branch> bestBranch = branches.stream()
                .filter(branch -> branchService.doesBranchContainCommit(branch, release.getOid()))
                .max(Comparator.comparing(branch -> "master".equals(branch.getName()) ? 1 : 0));

        bestBranch.ifPresentOrElse(
                release::setBranch, () -> log.warn("No matching branch found for release '{}'.", release.getTagName()));

        return release;
    }

    private Release setNewCommitsForRelease(Release release) {
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
