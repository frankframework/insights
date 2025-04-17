package org.frankframework.insights.release;

import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.branch.Branch;
import org.frankframework.insights.branch.BranchService;
import org.frankframework.insights.commit.Commit;
import org.frankframework.insights.common.entityconnection.branchcommit.BranchCommit;
import org.frankframework.insights.common.entityconnection.branchpullrequest.BranchPullRequest;
import org.frankframework.insights.common.entityconnection.releasecommit.ReleaseCommit;
import org.frankframework.insights.common.entityconnection.releasecommit.ReleaseCommitRepository;
import org.frankframework.insights.common.entityconnection.releasepullrequest.ReleasePullRequest;
import org.frankframework.insights.common.entityconnection.releasepullrequest.ReleasePullRequestRepository;
import org.frankframework.insights.common.mapper.Mapper;
import org.frankframework.insights.github.GitHubClient;
import org.frankframework.insights.github.GitHubRepositoryStatisticsService;
import org.frankframework.insights.pullrequest.PullRequest;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReleaseService {

    private final GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService;
    private final GitHubClient gitHubClient;
    private final Mapper mapper;
    private final ReleaseRepository releaseRepository;
    private final BranchService branchService;
    private final ReleaseCommitRepository releaseCommitRepository;
    private final ReleasePullRequestRepository releasePullRequestRepository;

    public ReleaseService(
            GitHubRepositoryStatisticsService gitHubRepositoryStatisticsService,
            GitHubClient gitHubClient,
            Mapper mapper,
            ReleaseRepository releaseRepository,
            BranchService branchService,
            ReleaseCommitRepository releaseCommitRepository,
            ReleasePullRequestRepository releasePullRequestRepository) {
        this.gitHubRepositoryStatisticsService = gitHubRepositoryStatisticsService;
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.releaseRepository = releaseRepository;
        this.branchService = branchService;
        this.releaseCommitRepository = releaseCommitRepository;
        this.releasePullRequestRepository = releasePullRequestRepository;
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
            List<Branch> allBranches = branchService.getAllBranches();

            Map<String, Set<BranchCommit>> commitsByBranchMap = branchService.getBranchCommitsByBranches(allBranches);

            Set<Release> releasesWithBranches = releaseDTOs.stream()
                    .map(dto -> createReleaseWithBranch(dto, allBranches, commitsByBranchMap))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            if (releasesWithBranches.isEmpty()) {
                log.info("No releases with detected base branch to inject.");
                return;
            }

            saveAllReleases(releasesWithBranches);

            Map<String, Set<BranchPullRequest>> pullRequestsByBranchMap =
                    branchService.getBranchPullRequestsByBranches(allBranches);
            assignCommitsAndPRsToReleases(releasesWithBranches, commitsByBranchMap, pullRequestsByBranchMap);
        } catch (Exception e) {
            throw new ReleaseInjectionException("Error injecting GitHub releases.", e);
        }
    }

    private Release createReleaseWithBranch(
            ReleaseDTO dto, List<Branch> branches, Map<String, Set<BranchCommit>> commitsByBranch) {
        if (dto.getTagCommit() == null || dto.getTagCommit().getCommitSha() == null || branches.isEmpty()) {
            log.warn("Skipping release '{}' due to missing info.", dto.getTagName());
            return null;
        }

        Release release = mapper.toEntity(dto, Release.class);
        release.setBranch(findBranchForRelease(release, branches, commitsByBranch));

        if (release.getBranch() == null) {
            log.warn("No matching branch found for release '{}'.", dto.getTagName());
            return null;
        }

        return release;
    }

    private Branch findBranchForRelease(
            Release release, List<Branch> branches, Map<String, Set<BranchCommit>> commitsByBranch) {
        return branches.stream()
                .filter(branch -> branchService.doesBranchContainCommit(
                        branch.getName(), commitsByBranch.get(branch.getId()), release.getCommitSha()))
                .max(Comparator.comparing(branch -> "master".equals(branch.getName()) ? 1 : 0))
                .orElse(null);
    }

    private void assignCommitsAndPRsToReleases(
            Set<Release> releases,
            Map<String, Set<BranchCommit>> commitsByBranch,
            Map<String, Set<BranchPullRequest>> pullRequestsByBranch) {
        Map<Branch, List<Release>> releasesByBranch = releases.stream()
                .filter(release -> release.getBranch() != null)
                .collect(Collectors.groupingBy(Release::getBranch));

        for (Map.Entry<Branch, List<Release>> entry : releasesByBranch.entrySet()) {
            Branch branch = entry.getKey();
            List<Release> branchReleases = entry.getValue();

            branchReleases.sort(Comparator.comparing(Release::getPublishedAt));

            for (Release release : branchReleases) {
                Set<Commit> newCommits = new HashSet<>();
                Set<PullRequest> newPullRequests = new HashSet<>();
                Release previousRelease = branchReleases.stream()
                        .filter(r -> r.getPublishedAt().isBefore(release.getPublishedAt()))
                        .max(Comparator.comparing(Release::getPublishedAt))
                        .orElse(null);

                if (previousRelease == null) {
                    newCommits.addAll(getCommitsBeforeRelease(branch, release, commitsByBranch));
                    newPullRequests.addAll(getPullRequestsBeforeRelease(branch, release, pullRequestsByBranch));
                } else {
                    newCommits.addAll(getCommitsBetweenReleases(branch, previousRelease, release, commitsByBranch));
                    newPullRequests.addAll(
                            getPullRequestsBetweenReleases(branch, previousRelease, release, pullRequestsByBranch));
                }

                if (!newCommits.isEmpty()) {
                    Set<ReleaseCommit> newReleaseCommits = newCommits.stream()
                            .map(commit -> new ReleaseCommit(release, commit))
                            .collect(Collectors.toSet());
                    releaseCommitRepository.saveAll(newReleaseCommits);
                }

                if (!newPullRequests.isEmpty()) {
                    Set<ReleasePullRequest> newReleasePullRequests = newPullRequests.stream()
                            .map(pr -> new ReleasePullRequest(release, pr))
                            .collect(Collectors.toSet());
                    releasePullRequestRepository.saveAll(newReleasePullRequests);
                }
            }
        }
    }

    private Set<Commit> getCommitsBeforeRelease(
            Branch branch, Release release, Map<String, Set<BranchCommit>> commitsByBranch) {
        return commitsByBranch.getOrDefault(branch.getId(), Set.of()).stream()
                .map(BranchCommit::getCommit)
                .filter(commit -> commit.getCommittedDate().isBefore(release.getPublishedAt()))
                .collect(Collectors.toSet());
    }

    private Set<Commit> getCommitsBetweenReleases(
            Branch branch, Release previousRelease, Release release, Map<String, Set<BranchCommit>> commitsByBranch) {
        return commitsByBranch.getOrDefault(branch.getId(), Set.of()).stream()
                .map(BranchCommit::getCommit)
                .filter(commit -> commit.getCommittedDate().isAfter(previousRelease.getPublishedAt())
                        && commit.getCommittedDate().isBefore(release.getPublishedAt()))
                .collect(Collectors.toSet());
    }

    private Set<PullRequest> getPullRequestsBeforeRelease(
            Branch branch, Release release, Map<String, Set<BranchPullRequest>> pullRequestsByBranch) {
        return pullRequestsByBranch.getOrDefault(branch.getId(), Set.of()).stream()
                .map(BranchPullRequest::getPullRequest)
                .filter(pr -> pr.getMergedAt().isBefore(release.getPublishedAt()))
                .collect(Collectors.toSet());
    }

    private Set<PullRequest> getPullRequestsBetweenReleases(
            Branch branch,
            Release previousRelease,
            Release release,
            Map<String, Set<BranchPullRequest>> pullRequestsByBranch) {
        return pullRequestsByBranch.getOrDefault(branch.getId(), Set.of()).stream()
                .map(BranchPullRequest::getPullRequest)
                .filter(pr -> pr.getMergedAt().isAfter(previousRelease.getPublishedAt())
                        && pr.getMergedAt().isBefore(release.getPublishedAt()))
                .collect(Collectors.toSet());
    }

    private void saveAllReleases(Set<Release> releases) {
        List<Release> savedReleases = releaseRepository.saveAll(releases);
        log.info("Successfully saved {} releases.", savedReleases.size());
    }
}
