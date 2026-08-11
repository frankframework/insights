package org.frankframework.insights.release;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.frankframework.insights.common.mapper.Mapper;
import org.springframework.stereotype.Service;

/**
 * Service class for reading releases from the database.
 * Filling the release tables is the responsibility of the data import module.
 */
@Service
@Slf4j
public class ReleaseQueryService {

    private final Mapper mapper;
    private final ReleaseRepository releaseRepository;

    public ReleaseQueryService(Mapper mapper, ReleaseRepository releaseRepository) {
        this.mapper = mapper;
        this.releaseRepository = releaseRepository;
    }

    /**
     * Retrieves all releases from the database.
     *
     * @return A set of ReleaseResponse DTOs representing all releases.
     */
    public Set<ReleaseResponse> getAllReleases() {
        Set<ReleaseResponse> releaseResponses = releaseRepository.findAll().stream()
                .map(r -> mapper.toDTO(r, ReleaseResponse.class))
                .collect(Collectors.toSet());

        log.info("Successfully fetched and mapped {} releases from the database", releaseResponses.size());
        return releaseResponses;
    }

    /**
     * Retrieves a single release by ID from the database.
     * @param releaseId the ID of the release to retrieve
     * @return a ReleaseResponse DTO representing the release
     * @throws ReleaseNotFoundException if the release does not exist
     */
    public ReleaseResponse getReleaseById(String releaseId) throws ReleaseNotFoundException {
        Release release = checkIfReleaseExists(releaseId);
        return mapper.toDTO(release, ReleaseResponse.class);
    }

    /**
     * Checks if a release with the given ID exists in the database.
     * @param releaseId the ID of the release to check
     * @return the Release object if it exists
     * @throws ReleaseNotFoundException if the release does not exist
     */
    public Release checkIfReleaseExists(String releaseId) throws ReleaseNotFoundException {
        Optional<Release> release = releaseRepository.findById(releaseId);

        if (release.isEmpty()) {
            release = releaseRepository.findByTagName(releaseId);
        }

        if (release.isEmpty()) {
            release = releaseRepository.findByTagName("release/" + releaseId);
        }

        if (release.isEmpty()) {
            throw new ReleaseNotFoundException("Release with ID [" + releaseId + "] not found.", null);
        }

        return release.get();
    }
}
