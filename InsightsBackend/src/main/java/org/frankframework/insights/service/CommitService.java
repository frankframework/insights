package org.frankframework.insights.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.dto.CommitDTO;
import org.frankframework.insights.mapper.CommitMapper;
import org.frankframework.insights.models.Commit;
import org.frankframework.insights.models.Release;
import org.frankframework.insights.repository.CommitRepository;
import org.springframework.stereotype.Service;

@Service
public class CommitService {
    private final GitHubClient gitHubClient;
    private final CommitMapper commitMapper;
    private final CommitRepository commitRepository;
    private final ReleaseService releaseService;

    public CommitService(
            GitHubClient gitHubClient,
            CommitMapper commitMapper,
            CommitRepository commitRepository,
            ReleaseService releaseService) {
        this.gitHubClient = gitHubClient;
        this.commitMapper = commitMapper;
        this.commitRepository = commitRepository;
        this.releaseService = releaseService;
    }

    public void injectCommits() throws RuntimeException {
        if (!commitRepository.findAll().isEmpty()) {
            return;
        }

        try {
            List<Release> releases = releaseService.getAllReleases();
            Set<Commit> allCommits = new HashSet<>();

            for (Release release : releases) {
                Set<CommitDTO> commitDTOS = gitHubClient.getCommits(release.getTagName());

                Set<Commit> commits = commitMapper.toEntity(commitDTOS);

                release.setCommits(commits);
                releaseService.saveOrUpdateRelease(release);

                allCommits.addAll(commits);
            }

            saveCommits(allCommits);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching commits", e);
        }
    }

    private void saveCommits(Set<Commit> commits) {
        Set<String> existingCommitIds =
                commitRepository.findAll().stream().map(Commit::getId).collect(Collectors.toSet());

        Set<Commit> newCommits = commits.stream()
                .filter(commit -> !existingCommitIds.contains(commit.getId()))
                .collect(Collectors.toSet());

        if (!newCommits.isEmpty()) {
            commitRepository.saveAll(newCommits);
        }
    }
}
