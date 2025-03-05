package org.frankframework.insights.service;

import java.util.List;
import java.util.Set;
import org.frankframework.insights.clients.GitHubClient;
import org.frankframework.insights.dto.ReleaseDTO;
import org.frankframework.insights.mapper.ReleaseMapper;
import org.frankframework.insights.models.Release;
import org.frankframework.insights.repository.ReleaseRepository;
import org.springframework.stereotype.Service;

@Service
public class ReleaseService {
    private final GitHubClient gitHubClient;

    private final ReleaseMapper releaseMapper;

    private final ReleaseRepository releaseRepository;

    public ReleaseService(GitHubClient gitHubClient, ReleaseMapper releaseMapper, ReleaseRepository releaseRepository) {
        this.gitHubClient = gitHubClient;
        this.releaseMapper = releaseMapper;
        this.releaseRepository = releaseRepository;
    }

    public void injectReleases() {
        if (!releaseRepository.findAll().isEmpty()) {
            return;
        }

        try {
            Set<ReleaseDTO> releaseDTOs = gitHubClient.getReleases();

            Set<Release> releases = releaseMapper.toEntity(releaseDTOs);

            saveReleases(releases);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Release> getAllReleases() {
        return releaseRepository.findAll();
    }

    private void saveReleases(Set<Release> releases) {
        releaseRepository.saveAll(releases);
    }

    public void saveOrUpdateRelease(Release release) {
        releaseRepository.save(release);
    }
}
