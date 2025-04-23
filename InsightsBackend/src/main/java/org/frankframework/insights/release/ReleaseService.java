package org.frankframework.insights.release;

import java.time.OffsetDateTime;
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

    private final GitHubRepositoryStatisticsService statisticsService;
    private final GitHubClient gitHubClient;
    private final Mapper mapper;
    private final ReleaseRepository releaseRepository;
    private final BranchService branchService;
    private final ReleaseCommitRepository releaseCommitRepository;
    private final ReleasePullRequestRepository releasePullRequestRepository;

    public ReleaseService(
            GitHubRepositoryStatisticsService statisticsService,
            GitHubClient gitHubClient,
            Mapper mapper,
            ReleaseRepository releaseRepository,
            BranchService branchService,
            ReleaseCommitRepository releaseCommitRepository,
            ReleasePullRequestRepository releasePullRequestRepository) {
        this.statisticsService = statisticsService;
        this.gitHubClient = gitHubClient;
        this.mapper = mapper;
        this.releaseRepository = releaseRepository;
        this.branchService = branchService;
        this.releaseCommitRepository = releaseCommitRepository;
        this.releasePullRequestRepository = releasePullRequestRepository;
    }

    public void injectReleases() throws ReleaseInjectionException {
        if (statisticsService.getGitHubRepositoryStatisticsDTO().getGitHubReleaseCount() == releaseRepository.count()) {
            log.info("Releases already exist in the database.");
            return;
        }

        try {
            Set<ReleaseDTO> releaseDTOs = gitHubClient.getReleases();
            List<Branch> allBranches = branchService.getAllBranches();
            Map<String, Set<BranchCommit>> commitsByBranch = branchService.getBranchCommitsByBranches(allBranches);
            Map<String, Set<BranchPullRequest>> pullRequestsByBranch =
                    branchService.getBranchPullRequestsByBranches(allBranches);

            Set<Release> releases = releaseDTOs.stream()
                    .map(dto -> mapToRelease(dto, allBranches, commitsByBranch))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            if (releases.isEmpty()) {
                log.info("No valid releases found.");
                return;
            }

            saveAllReleases(releases);

            Map<Branch, List<Release>> releasesByBranch = releases.stream()
                    .filter(r -> r.getBranch() != null)
                    .collect(Collectors.groupingBy(
                            Release::getBranch, Collectors.collectingAndThen(Collectors.toList(), list -> list.stream()
                                    .sorted(Comparator.comparing(Release::getPublishedAt))
                                    .collect(Collectors.toList()))));

            processAndAssignPullsAndCommits(releasesByBranch, commitsByBranch, pullRequestsByBranch);
        } catch (Exception e) {
            throw new ReleaseInjectionException("Error injecting GitHub releases.", e);
        }
    }

    private Release mapToRelease(ReleaseDTO dto, List<Branch> branches, Map<String, Set<BranchCommit>> commitsMap) {
        String sha = Optional.ofNullable(dto.getTagCommit())
                .map(ReleaseTagCommitDTO::getCommitSha)
                .orElse(null);
        if (sha == null) return null;

        return branches.stream()
                .filter(b -> commitsMap.getOrDefault(b.getId(), Set.of()).stream()
                        .map(BranchCommit::getCommit)
                        .anyMatch(c -> sha.equals(c.getSha())))
                .findFirst()
                .map(branch -> {
                    Release release = mapper.toEntity(dto, Release.class);
                    release.setBranch(branch);
                    return release;
                })
                .orElse(null);
    }

    private void processAndAssignPullsAndCommits(
            Map<Branch, List<Release>> releasesByBranch,
            Map<String, Set<BranchCommit>> commitsByBranch,
            Map<String, Set<BranchPullRequest>> pullRequestsByBranch) {

        List<Release> masterReleases = new ArrayList<>();
        Branch masterBranch = releasesByBranch.keySet().stream()
                .filter(b -> "master".equalsIgnoreCase(b.getName()))
                .findFirst()
                .orElse(null);

        for (Map.Entry<Branch, List<Release>> entry : releasesByBranch.entrySet()) {
            Branch branch = entry.getKey();
            List<Release> releases = entry.getValue();
            if ("master".equalsIgnoreCase(branch.getName())) continue;

            Set<BranchCommit> commits = commitsByBranch.getOrDefault(branch.getId(), Set.of());
            Set<BranchPullRequest> prs = pullRequestsByBranch.getOrDefault(branch.getId(), Set.of());

            if (releases.size() == 1) {
                masterReleases.add(releases.getFirst());
            } else {
                List<Release> sortedReleases = assignToReleases(releases, commits, prs);
                masterReleases.add(sortedReleases.getFirst());
            }
        }

        if (masterBranch != null) {
            List<Release> masterOnly = releasesByBranch.getOrDefault(masterBranch, List.of());
            List<Release> combined = new ArrayList<>(masterReleases);
            combined.addAll(masterOnly);
            combined.sort(Comparator.comparing(Release::getPublishedAt));

            Set<BranchCommit> masterCommits = commitsByBranch.getOrDefault(masterBranch.getId(), Set.of());
            Set<BranchPullRequest> masterPRs = pullRequestsByBranch.getOrDefault(masterBranch.getId(), Set.of());

            assignToReleases(combined, masterCommits, masterPRs);
        }
    }

    private List<Release> assignToReleases(
            List<Release> releases, Set<BranchCommit> commits, Set<BranchPullRequest> prs) {
        for (int i = 0; i < releases.size(); i++) {
            Release current = releases.get(i);

            if (i > 0) {
                OffsetDateTime from = releases.get(i - 1).getPublishedAt();
                OffsetDateTime to = current.getPublishedAt();
                assignCommits(current, commits, from, to);
                assignPullRequests(current, prs, from, to);
            }
        }

        return releases.stream()
                .sorted(Comparator.comparing(Release::getPublishedAt))
                .collect(Collectors.toList());
    }

    private void assignCommits(
            Release release, Set<BranchCommit> branchCommits, OffsetDateTime from, OffsetDateTime to) {
        Set<Commit> commits = branchCommits.stream()
                .map(BranchCommit::getCommit)
                .filter(c -> isInRange(c.getCommittedDate(), from, to))
                .collect(Collectors.toSet());

        if (!commits.isEmpty()) {
            releaseCommitRepository.saveAll(
                    commits.stream().map(c -> new ReleaseCommit(release, c)).collect(Collectors.toSet()));
        }
    }

    private void assignPullRequests(
            Release release, Set<BranchPullRequest> branchPRs, OffsetDateTime from, OffsetDateTime to) {
        Set<PullRequest> prs = branchPRs.stream()
                .map(BranchPullRequest::getPullRequest)
                .filter(p -> isInRange(p.getMergedAt(), from, to))
                .collect(Collectors.toSet());

        if (!prs.isEmpty()) {
            releasePullRequestRepository.saveAll(
                    prs.stream().map(p -> new ReleasePullRequest(release, p)).collect(Collectors.toSet()));
        }
    }

    private boolean isInRange(OffsetDateTime date, OffsetDateTime start, OffsetDateTime end) {
        return date != null && (date.isEqual(start) || date.isAfter(start)) && date.isBefore(end);
    }

    private void saveAllReleases(Set<Release> releases) {
        List<Release> savedReleases = releaseRepository.saveAll(releases);
        log.info("Saved {} releases.", savedReleases.size());
    }

    public Set<ReleaseResponse> getAllReleases() {
        return releaseRepository.findAll().stream()
                .map(r -> mapper.toDTO(r, ReleaseResponse.class))
                .collect(Collectors.toSet());
    }

    public ReleaseResponse getReleaseById(String id) throws ReleaseNotFoundException {
        return mapper.toDTO(
                releaseRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ReleaseNotFoundException("Release with ID [" + id + "] not found.", null)),
                ReleaseResponse.class);
    }

    public Release checkIfReleaseExists(String releaseId) throws ReleaseNotFoundException {
        return releaseRepository
                .findById(releaseId)
                .orElseThrow(() -> new ReleaseNotFoundException("Release was not found.", null));
    }
}
